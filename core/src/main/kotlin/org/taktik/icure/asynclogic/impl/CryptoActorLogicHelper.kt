package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.toList
import org.taktik.couchdb.entity.Versionable
import org.taktik.icure.asyncdao.GenericDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.base.effectiveGroupLinkType
import org.taktik.icure.exceptions.ConflictRequestException

/**
 * Shared logic-layer enforcement of [CryptoActor.groupLinkType] immutability and [CryptoActor.dataOwnerGroups]/
 * legacy `parentId` link validation and storage-shape normalization, for [CryptoActor] implementers backed by their
 * own type-specific DAO (currently [org.taktik.icure.entities.HealthcareParty] and [org.taktik.icure.entities.Device]).
 * A subclass only needs to say what [DataOwnerType] it represents ([dataOwnerType]) and how to copy the normalized
 * links back onto an instance of [T] ([copyWithLinks]) — everything else is identical between them.
 */
abstract class CryptoActorLogicHelper<T, D : GenericDAO<T>>(
	private val dao: D,
) where T : CryptoActor, T : Versionable<String> {
	protected abstract val dataOwnerType: DataOwnerType

	protected abstract fun T.copyWithLinks(parentId: String?, dataOwnerGroups: List<DataOwnerGroupLink>): T

	/**
	 * Enforces [CryptoActor.groupLinkType] immutability (unconditional, no permission can bypass it), validates the
	 * [entity]'s [CryptoActor.dataOwnerGroups]/legacy `parentId` link targets, and returns [entity] with its
	 * `parentId`/`dataOwnerGroups` replaced by the normalized storage shape that should actually be persisted.
	 *
	 * Every [CryptoActor.dataOwnerGroups] entry is validated strictly, on every call: its target must exist (found
	 * through [dao], so a wrong-type id is indistinguishable from a missing one), its effective type must not be
	 * [DataOwnerGroupLinkType.notAllowed], and its effective [DataOwnerGroupLinkType.strength] must not be greater
	 * than [entity]'s own effective strength — a data owner may only link to targets that are the same strength as
	 * itself or weaker, never stronger. This is the same "a weak thing cannot point at a strong thing" rule the
	 * hierarchy resolver enforces at resolve time (see `HcpHierarchyResolver.canTransitivelyFollow`), just applied
	 * here at write time, on a single edge, rather than at resolve time, along a full path. Phrasing it in terms of
	 * strength rather than naming [DataOwnerGroupLinkType.simple]/[DataOwnerGroupLinkType.parent] directly keeps the
	 * rule correct if a third, intermediate-strength link type is ever added — with today's two non-`notAllowed`
	 * types this happens to coincide with "a `simple`-effective source may only link to `simple`-effective targets"
	 * (a `parent`-effective or `notAllowed`-effective source is unconstrained, since nothing is stronger than
	 * `parent` besides `notAllowed`, which is separately excluded above).
	 *
	 * The legacy `parentId`, on the other hand, is validated leniently, matching its long legacy history of never
	 * being validated at all: only if it changed and the target can actually be found is it required to be
	 * [DataOwnerGroupLinkType.parent]-effective; a missing target, or an unchanged `parentId`, is tolerated with no
	 * error, and left exactly as declared with no attempt to reshape it.
	 *
	 * If `parentId` did not change, [entity] is returned with its links exactly as declared, with no attempt to
	 * validate or reshape them: normalization only kicks in when the caller is actively creating or changing
	 * `parentId`, not as a side effect of unrelated modifications. Otherwise, the full normalized link set (declared
	 * `dataOwnerGroups` plus `parentId`, deduped) is re-split: an explicit, validated `parentId` always keeps the
	 * storage slot; failing that, if exactly one target is parent-effective, it becomes the stored `parentId`
	 * (preferring the legacy representation, as this is more broadly compatible); failing that (zero or several
	 * candidates, and no explicit `parentId`, e.g. a cardinal 3+ client which never populates the legacy field), the
	 * stored `parentId` is [original]'s `parentId` if it is still one of them (preserving its identity across edits
	 * instead of arbitrarily picking one or dropping it), or `null` otherwise.
	 *
	 * @param entity the entity being created or modified, with its links exactly as declared by the caller.
	 * @param original the entity's previously stored version, or `null` on creation.
	 * @throws IllegalArgumentException per the rules above, or if [original]'s nullness is inconsistent with
	 * whether [entity] already has a `rev` (a caller-wiring guard: a create call should never already have a
	 * `rev`, and a modify call should always have one).
	 * @throws ConflictRequestException if [entity]'s `rev` doesn't match [original]'s -- checked immediately, before
	 * any of the validation/normalization work below, since the eventual save would fail on this same mismatch
	 * anyway; no point doing DAO lookups for a modification that's already doomed to a conflict.
	 */
	suspend fun validateAndNormalizeOwnGroupLinks(
		entity: T,
		original: T?,
		datastoreInformation: IDatastoreInformation,
	): T {
		require((original == null) == (entity.rev == null)) {
			"Inconsistent create/modify call: original is ${if (original == null) "null (create)" else "non-null (modify)"}" +
				" but entity.rev is ${if (entity.rev == null) "null" else "non-null"}."
		}
		if (original != null) {
			if (entity.rev != original.rev) {
				throw ConflictRequestException("Rev of original entity is ${original.rev} but rev of updated is ${entity.rev}.")
			}
			require(original.groupLinkType == entity.groupLinkType) {
				"The groupLinkType of a data owner can never be changed once set (or once relied upon as null)."
			}
		}
		val declaredParentId = entity.parentId
		// True both when creating (there is no original parentId to compare against) and when modifying with an
		// actually different parentId -- never true for a modification that leaves parentId untouched.
		val isNewOrParentIdChanged = original == null || original.parentId != declaredParentId
		val allDeclaredIds = entity.dataOwnerGroups.mapTo(mutableSetOf()) { it.dataOwnerId }.apply { declaredParentId?.let(::add) }
		val effectiveTypesById = if (allDeclaredIds.isEmpty()) {
			emptyMap()
		} else {
			dao.getEntities(datastoreInformation, allDeclaredIds.toList()).toList().associate { it.id to it.effectiveGroupLinkType(dataOwnerType) }
		}

		val ownEffectiveType = entity.effectiveGroupLinkType(dataOwnerType)
		entity.dataOwnerGroups.forEach { link ->
			val effectiveType = effectiveTypesById[link.dataOwnerId]
				?: throw IllegalArgumentException(
					"Cannot link to data owner ${link.dataOwnerId}: it does not exist, or is not of the same type.",
				)
			require(effectiveType != DataOwnerGroupLinkType.notAllowed) {
				"Cannot link to data owner ${link.dataOwnerId}: it is not allowed to be a group target."
			}
			require(effectiveType.strength <= ownEffectiveType.strength) {
				"Cannot link to data owner ${link.dataOwnerId}: its effective type ($effectiveType) is stronger than this data owner's own effective type ($ownEffectiveType) -- a data owner may only link to targets that are the same strength as itself or weaker."
			}
		}

		val (newParentId, newDataOwnerGroups) = if (isNewOrParentIdChanged) {
			normalizeParentId(entity, original, declaredParentId, effectiveTypesById)
		} else {
			// An unchanged parentId is never re-validated (per the leniency above) -- and, symmetrically, never
			// reshaped either: normalization only kicks in when the caller is actively creating or changing
			// parentId, not as a side effect of unrelated modifications (e.g. touching only dataOwnerGroups, or
			// another field entirely). This also naturally tolerates a parentId whose target can't be found, or is
			// no longer parent-effective, without trying to move it into dataOwnerGroups.
			declaredParentId to entity.dataOwnerGroups
		}
		return entity.copyWithLinks(newParentId, newDataOwnerGroups)
	}

	/**
	 * Validates a changed [declaredParentId] and computes the `(parentId, dataOwnerGroups)` storage split — only
	 * called when the caller is actively creating or changing `parentId`, see
	 * [validateAndNormalizeOwnGroupLinks].
	 */
	private fun normalizeParentId(
		entity: T,
		original: T?,
		declaredParentId: String?,
		effectiveTypesById: Map<String, DataOwnerGroupLinkType>,
	): Pair<String?, List<DataOwnerGroupLink>> {
		if (declaredParentId != null) {
			effectiveTypesById[declaredParentId]?.let { effectiveType ->
				require(effectiveType == DataOwnerGroupLinkType.parent) {
					"Cannot set parentId to $declaredParentId: it is not a parent-effective data owner."
				}
			}
			if (declaredParentId !in effectiveTypesById) {
				// Dangling parentId, tolerated per the leniency above: pass through unchanged rather than reshaping
				// around a target we know nothing about.
				return declaredParentId to entity.dataOwnerGroups.filter { it.dataOwnerId != declaredParentId }
			}
		}
		val normalizedLinks = (entity.dataOwnerGroups + listOfNotNull(declaredParentId?.let(::DataOwnerGroupLink))).distinctBy { it.dataOwnerId }
		val parentEffectiveIds = normalizedLinks.map { it.dataOwnerId }.filter { effectiveTypesById[it] == DataOwnerGroupLinkType.parent }
		val originalParentId = original?.parentId
		val newParentId = when {
			// An explicit, validated parentId always wins the storage slot.
			declaredParentId != null && declaredParentId in parentEffectiveIds -> declaredParentId
			parentEffectiveIds.size == 1 -> parentEffectiveIds.single()
			// declaredParentId is null (e.g. a cardinal 3+ client, which never populates the legacy field,
			// submitting every link through dataOwnerGroups instead) and there's more than one parent-effective
			// candidate: prefer the previously stored parentId's target, if it's still one of them, to preserve its
			// identity across edits rather than arbitrarily picking one or dropping it.
			originalParentId != null && originalParentId in parentEffectiveIds -> originalParentId
			else -> null
		}
		return newParentId to normalizedLinks.filter { it.dataOwnerId != newParentId }
	}
}
