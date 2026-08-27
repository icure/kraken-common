package org.taktik.icure.security

import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.base.DataOwnerHierarchyInfo
import org.taktik.icure.entities.base.effectiveGroupLinkType
import org.taktik.icure.entities.base.isAtLeast
import org.taktik.icure.exceptions.IllegalEntityException

/**
 * Safety limit on the number of distinct ancestor groups [resolveHcpAncestors] will follow for a single healthcare
 * party, to avoid unbounded work on a pathologically large or adversarial hierarchy.
 */
private const val MAX_HCP_ANCESTORS = 100

/**
 * Resolves all the (transitive) ancestor groups (parents, organisations, locations, ...) that [childHcp] is a member
 * of, following the legacy [HealthcareParty.parentId] link plus all [HealthcareParty.dataOwnerGroups] links.
 *
 * Membership propagates through all links, whatever their type: the groups of a group joined through a link are
 * included in the result, recursively.
 *
 * The type of a link is intrinsic to its *target* (see [org.taktik.icure.entities.base.CryptoActor.effectiveGroupLinkType]),
 * not declared by whoever links to it. Every [DataOwnerGroupLinkType] has a [DataOwnerGroupLinkType.strength]. Along
 * any single path away from [childHcp], the strength of successive links may only stay the same (with the same link
 * type) or decrease: a link stronger than the one before it, or of the same strength but a different type
 * ("shifting"), makes the transitive link ambiguous and is rejected. This is intentionally conservative — a single
 * ambiguous link fails the whole resolution, even if the same target is also reachable through another, unambiguous
 * path (diamond configurations) — since there is no current use case for the more permissive alternative (e.g.
 * resolving the transitive type from the least-restrictive valid path). This restriction may be relaxed in the
 * future.
 *
 * A group reachable through different paths (diamond configurations) is legal and appears exactly once, but a
 * circular reference along a single path causes an [IllegalEntityException]. Direct self-references and links to
 * healthcare parties that cannot be loaded are ignored. A blank legacy [HealthcareParty.parentId] is also tolerated,
 * treated as if it were absent, for compatibility with legacy data (see [declaredGroupLinkIds]); a blank
 * [HealthcareParty.dataOwnerGroups] entry id, on the other hand, is always an error.
 *
 * @param childHcp the healthcare party to resolve the ancestor groups of.
 * @param loadHealthcareParties loads the healthcare parties with the provided ids, omitting the ids that do not
 * match any existing healthcare party.
 * @param minAcceptedType if not null, only links whose *target's* effective type is at least this strong (see
 * [org.taktik.icure.entities.base.isAtLeast]) are followed; all other links (including any `notAllowed`-effective
 * target, which should never actually occur) are ignored as if they were not present. Since a target's effective
 * type is only known once it is loaded, the load phase always loads every declared target regardless of this
 * restriction — only the build phase filters by it, so this has no effect on how many healthcare parties
 * [loadHealthcareParties] is asked to load.
 * @return the ancestor groups of [childHcp], deduplicated, excluding [childHcp] itself, in depth-first
 * first-encounter order following the declaration order of the links.
 * @throws IllegalEntityException if a `dataOwnerGroups` entry with a blank id or a circular reference is found, if
 * the number of distinct ancestor groups exceeds [MAX_HCP_ANCESTORS], or if a link stronger than (or of the same
 * strength as, but a different type than) the previous link on the same path is found.
 */
suspend fun resolveHcpAncestors(
	childHcp: HealthcareParty,
	minAcceptedType: DataOwnerGroupLinkType? = null,
	loadHealthcareParties: suspend (Set<String>) -> Collection<HealthcareParty>,
): List<HealthcareParty> {
	val loadedById = mutableMapOf(childHcp.id to childHcp)
	val expandedIds = mutableSetOf(childHcp.id)
	var frontier = listOf(childHcp)
	while (frontier.isNotEmpty()) {
		val links = frontier.flatMap { it.declaredGroupLinkIds(childHcp) }
		val idsToLoad = links.toSet() - loadedById.keys
		if (idsToLoad.isNotEmpty()) {
			loadHealthcareParties(idsToLoad).forEach { loadedById[it.id] = it }
		}
		frontier = links.filter { expandedIds.add(it) }.mapNotNull { loadedById[it] }
		if (expandedIds.size - 1 > MAX_HCP_ANCESTORS) {
			throw IllegalEntityException(
				"Too many ancestor groups for healthcare party ${childHcp.id}: exceeds the maximum of $MAX_HCP_ANCESTORS",
			)
		}
	}

	val ancestors = LinkedHashMap<String, HealthcareParty>()
	fun visit(of: HealthcareParty, pathIds: Set<String>, lastLinkType: DataOwnerGroupLinkType?) {
		of.groupLinksWithEffectiveType(childHcp, loadedById, minAcceptedType).forEach { (linkType, groupId) ->
			when {
				groupId == of.id -> {} // tolerated for compatibility with the legacy parentId handling
				groupId in pathIds -> throw IllegalEntityException(
					"Circular reference in the hcp hierarchy starting from ${childHcp.id} detected.",
				)
				lastLinkType != null && !linkType.canTransitivelyFollow(lastLinkType) -> throw IllegalEntityException(
					"Ambiguous transitive data owner group link in the hcp hierarchy starting from ${childHcp.id}: " +
						"a $linkType link (strength ${linkType.strength}) follows a $lastLinkType link " +
						"(strength ${lastLinkType.strength}) on the path to $groupId.",
				)
				else -> loadedById[groupId]?.let { group ->
					if (ancestors.putIfAbsent(group.id, group) == null) {
						visit(group, pathIds + groupId, linkType)
					}
				}
			}
		}
	}
	visit(childHcp, setOf(childHcp.id), null)
	return ancestors.values.toList()
}

/**
 * The ids of the ancestor groups of a healthcare party, partitioned by the rights they grant.
 *
 * @property parentLinkedIds ids of the ancestor groups reachable exclusively through [DataOwnerGroupLinkType.parent]
 * links (including the legacy parentId): these grant administrative rights over the healthcare party.
 * @property simpleLinkedIds ids of the ancestor groups whose every path from the healthcare party includes at least
 * one [DataOwnerGroupLinkType.simple] link: these provide membership only and never grant administrative rights.
 * Disjoint from [parentLinkedIds].
 */
data class HcpAncestorIdsByRights(
	val parentLinkedIds: Set<String>,
	val simpleLinkedIds: Set<String>,
) {
	companion object {
		val EMPTY = HcpAncestorIdsByRights(emptySet(), emptySet())
	}
}

/**
 * Resolves the ids of all the ancestor groups of [childHcp] (same traversal as [resolveHcpAncestors] with no link
 * type restriction) partitioned by the rights they grant: the groups reachable through a pure
 * [DataOwnerGroupLinkType.parent] path and the groups only reachable through paths including an
 * [DataOwnerGroupLinkType.simple] link. A group reachable both ways counts as a parent.
 * The partitioning is done in memory on the ancestors loaded by the full traversal: [loadHealthcareParties] is
 * invoked exactly as many times as by a single [resolveHcpAncestors] call.
 *
 * Both id sets preserve insertion order (backed by a `LinkedHashSet`), topmost ancestor first: for a plain,
 * non-branching legacy `parentId` chain this matches the pre-multi-parent hierarchy claim ordering (topmost first,
 * direct parent last). Order has no single well-defined meaning once a hierarchy branches (multiple parents /
 * diamonds), so it should not be relied upon in that case.
 *
 * @throws IllegalEntityException if a `dataOwnerGroups` entry with a blank id or a circular reference is found,
 * or if the number of distinct ancestor groups exceeds [MAX_HCP_ANCESTORS].
 */
suspend fun resolveHcpAncestorIdsByRights(
	childHcp: HealthcareParty,
	loadHealthcareParties: suspend (Set<String>) -> Collection<HealthcareParty>,
): HcpAncestorIdsByRights {
	val simpleLinkedIds = resolveHcpAncestors(childHcp, null, loadHealthcareParties)
	val loadedById = simpleLinkedIds.associateBy { it.id }
	// resolveHcpAncestors returns direct-first, topmost-last (deterministic first-encounter order); reversed here so
	// that a plain, non-branching parentId chain keeps the pre-multi-parent JWT hierarchy order: topmost-first,
	// direct-parent-last. Order has no defined meaning for branching hierarchies, so this only matters for that case.
	val parentLinkedIds = resolveHcpAncestors(childHcp, DataOwnerGroupLinkType.parent) { ids ->
		ids.mapNotNull { loadedById[it] }
	}.asReversed().mapTo(LinkedHashSet()) { it.id }
	return HcpAncestorIdsByRights(
		parentLinkedIds = parentLinkedIds,
		simpleLinkedIds = simpleLinkedIds.asReversed().mapNotNullTo(LinkedHashSet()) { ancestor -> ancestor.id.takeIf { it !in parentLinkedIds } },
	)
}

/**
 * Same traversal as [resolveHcpAncestors] but returns the hierarchies of [childHcp] as a tree of ids rooted at
 * [childHcp] itself: the parents of each node are the groups it is directly linked to, together with the effective
 * type of the link it was reached through (i.e. the linked group's own type, see
 * [org.taktik.icure.entities.base.CryptoActor.effectiveGroupLinkType]). Contrary to [resolveHcpAncestors], a group
 * reachable through multiple paths (diamond configurations) appears once per path.
 *
 * @param childHcp the healthcare party to resolve the id hierarchies of.
 * @param loadHealthcareParties loads the healthcare parties with the provided ids, omitting the ids that do not
 * match any existing healthcare party.
 * @param minAcceptedType if not null, only links whose target's effective type is at least this strong (see
 * [org.taktik.icure.entities.base.isAtLeast]) are followed; all other links are ignored as if they were not present.
 * @return the id hierarchy tree rooted at [childHcp].
 * @throws IllegalEntityException if a `dataOwnerGroups` entry with a blank id or a circular reference is found,
 * or if the number of distinct ancestor groups exceeds [MAX_HCP_ANCESTORS].
 */
suspend fun resolveHcpHierarchyInfo(
	childHcp: HealthcareParty,
	minAcceptedType: DataOwnerGroupLinkType? = null,
	loadHealthcareParties: suspend (Set<String>) -> Collection<HealthcareParty>,
): DataOwnerHierarchyInfo {
	// Loads all the ancestors and validates the links (no cycle: the recursion below terminates)
	val ancestorsById = resolveHcpAncestors(childHcp, minAcceptedType, loadHealthcareParties).associateBy { it.id }
	fun nodesOf(hcp: HealthcareParty): List<DataOwnerHierarchyInfo.HierarchyNode> = hcp
		.groupLinksWithEffectiveType(childHcp, ancestorsById, minAcceptedType)
		.filter { (_, groupId) -> groupId != hcp.id }
		.mapNotNull { (linkType, groupId) ->
			ancestorsById[groupId]?.let { group ->
				DataOwnerHierarchyInfo.HierarchyNode(
					linkedGroupId = groupId,
					linkType = linkType,
					transitiveLinks = nodesOf(group),
				)
			}
		}
	return DataOwnerHierarchyInfo(id = childHcp.id, dataOwnerType = DataOwnerType.HCP, links = nodesOf(childHcp))
}

/**
 * The ids of the groups this healthcare party is directly linked to: the legacy [HealthcareParty.parentId] plus
 * every [HealthcareParty.dataOwnerGroups] entry, deduplicated. Used by the load phase, which only needs ids — the
 * type of each link is only known once its target is loaded, see [groupLinksWithEffectiveType].
 *
 * A blank (non-null but empty/whitespace) [HealthcareParty.parentId] is tolerated and treated as if it were absent,
 * for compatibility with legacy data that predates any validation on this field. [HealthcareParty.dataOwnerGroups]
 * has no such legacy baggage — it is validated strictly instead, see below.
 *
 * @throws IllegalEntityException if any [HealthcareParty.dataOwnerGroups] entry has a blank id.
 */
private fun HealthcareParty.declaredGroupLinkIds(childHcp: HealthcareParty): List<String> =
	(listOfNotNull(parentId?.takeIf { it.isNotBlank() }) + dataOwnerGroups.map { it.dataOwnerId }.onEach { groupId ->
		if (groupId.isBlank()) {
			throw IllegalEntityException("Blank group id for healthcare party ${childHcp.id}")
		}
	}).distinct()

/**
 * The groups this healthcare party is directly linked to (see [declaredGroupLinkIds]), together with the *effective*
 * type of each link — i.e. the linked group's own type ([org.taktik.icure.entities.base.CryptoActor.effectiveGroupLinkType]),
 * not anything declared by this healthcare party. Links to a group not present in [loadedById] (missing or not yet
 * loaded) are skipped, matching the "links to healthcare parties that cannot be loaded are ignored" tolerance.
 * Restricted to links whose target's effective type [isAtLeast] [minAcceptedType], if it is not null.
 */
private fun HealthcareParty.groupLinksWithEffectiveType(
	childHcp: HealthcareParty,
	loadedById: Map<String, HealthcareParty>,
	minAcceptedType: DataOwnerGroupLinkType?,
): List<Pair<DataOwnerGroupLinkType, String>> = declaredGroupLinkIds(childHcp)
	.mapNotNull { groupId -> loadedById[groupId]?.let { target -> target.effectiveGroupLinkType(DataOwnerType.HCP) to groupId } }
	.filter { (linkType, _) -> linkType.isAtLeast(minAcceptedType) }

/**
 * Whether a link of this type may transitively follow a link of type [previous] on the same path away from the
 * starting healthcare party: only allowed if this link's [DataOwnerGroupLinkType.strength] is lower than
 * [previous]'s, or the same and of the same type (see [resolveHcpAncestors]).
 */
private fun DataOwnerGroupLinkType.canTransitivelyFollow(previous: DataOwnerGroupLinkType): Boolean = strength < previous.strength || this == previous
