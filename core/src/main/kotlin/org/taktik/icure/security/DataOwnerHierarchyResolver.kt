package org.taktik.icure.security

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.CryptoActor
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
): List<HealthcareParty> = resolveDataOwnerAncestors(
	childDataOwner = childHcp,
	minAcceptedType = minAcceptedType,
	dataOwnerType = DataOwnerType.HCP,
	loadHealthcareParties = loadHealthcareParties,
)

suspend fun <T> resolveDataOwnerAncestors(
	childDataOwner: T,
	minAcceptedType: DataOwnerGroupLinkType? = null,
	dataOwnerType: DataOwnerType,
	loadHealthcareParties: suspend (Set<String>) -> Collection<T>,
): List<T> where T : CryptoActor, T : Identifiable<String> {
	val loadedById = mutableMapOf(childDataOwner.id to childDataOwner)
	val expandedIds = mutableSetOf(childDataOwner.id)
	var frontier = listOf(childDataOwner)
	while (frontier.isNotEmpty()) {
		val links = frontier.flatMap { it.declaredGroupLinkIds(childDataOwner) }
		val idsToLoad = links.toSet() - loadedById.keys
		if (idsToLoad.isNotEmpty()) {
			loadHealthcareParties(idsToLoad).forEach { loadedById[it.id] = it }
		}
		frontier = links.filter { expandedIds.add(it) }.mapNotNull { loadedById[it] }
		if (expandedIds.size - 1 > MAX_HCP_ANCESTORS) {
			throw IllegalEntityException(
				"Too many ancestor groups for healthcare party ${childDataOwner.id}: exceeds the maximum of $MAX_HCP_ANCESTORS",
			)
		}
	}

	val ancestors = LinkedHashMap<String, T>()
	fun visit(of: T, pathIds: Set<String>, lastLinkType: DataOwnerGroupLinkType?) {
		of.groupLinksWithEffectiveType(
			childDataOwner = childDataOwner,
			loadedById = loadedById,
			minAcceptedType = minAcceptedType,
			dataOwnerType = dataOwnerType
		).forEach { (linkType, groupId) ->
			when {
				groupId == of.id -> {} // tolerated for compatibility with the legacy parentId handling
				groupId in pathIds -> throw IllegalEntityException(
					"Circular reference in the hcp hierarchy starting from ${childDataOwner.id} detected.",
				)
				lastLinkType != null && !linkType.canTransitivelyFollow(lastLinkType) -> throw IllegalEntityException(
					"Ambiguous transitive data owner group link in the hcp hierarchy starting from ${childDataOwner.id}: " +
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
	visit(childDataOwner, setOf(childDataOwner.id), null)
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
): DataOwnerHierarchyInfo =
	resolveDataOwnerHierarchyInfo(
		childDataOwner = childHcp,
		minAcceptedType = minAcceptedType,
		dataOwnerType = DataOwnerType.HCP,
		loadParents = loadHealthcareParties,
	)

suspend fun <T> resolveDataOwnerHierarchyInfo(
	childDataOwner: T,
	minAcceptedType: DataOwnerGroupLinkType? = null,
	dataOwnerType: DataOwnerType,
	loadParents: suspend (Set<String>) -> Collection<T>,
): DataOwnerHierarchyInfo  where T : CryptoActor, T : Identifiable<String>  {
	// Loads all the ancestors and validates the links (no cycle: the recursion below terminates)
	val ancestorsById = resolveDataOwnerAncestors(
		childDataOwner = childDataOwner,
		minAcceptedType = minAcceptedType,
		loadHealthcareParties = loadParents,
		dataOwnerType = dataOwnerType
	).associateBy { it.id }
	fun nodesOf(dataOwner: T): List<DataOwnerHierarchyInfo.HierarchyNode> = dataOwner
		.groupLinksWithEffectiveType(childDataOwner, ancestorsById, minAcceptedType, dataOwnerType)
		.filter { (_, groupId) -> groupId != dataOwner.id }
		.mapNotNull { (linkType, groupId) ->
			ancestorsById[groupId]?.let { group ->
				DataOwnerHierarchyInfo.HierarchyNode(
					linkedGroupId = groupId,
					linkType = linkType,
					transitiveLinks = nodesOf(group),
				)
			}
		}
	return DataOwnerHierarchyInfo(id = childDataOwner.id, dataOwnerType = dataOwnerType, links = nodesOf(childDataOwner))
}

/**
 * Which of the data owners with id in [dataOwnerIds] are members of the data owner group with id
 * [dataOwnerGroupId], directly or transitively.
 *
 * This answers only that question, for a whole batch at once, and is deliberately cheaper than resolving the
 * hierarchy of each of them with [resolveDataOwnerAncestors] and looking for [dataOwnerGroupId] in the result:
 *
 * - a data owner of the batch is done as soon as any of the groups it belongs to declares a link to
 *   [dataOwnerGroupId]. Whatever is above that group, and whatever else it is linked to, is never followed for it.
 * - the whole batch is traversed together, one bulk load per level, and every data owner is loaded at most once
 *   however many members of the batch it stands above: asking whether A and B belong to D, when both are only
 *   linked to C, loads and follows C once.
 *
 * Membership is pure reachability here: a link makes its declarer a member of its target whatever the link's type,
 * and so does every link of that target, recursively. The type of a link decides which *rights* a membership grants
 * (see [resolveDataOwnerAncestors]), not whether there is one, so contrary to that method there is no restriction
 * on how the type may change along a path, and no [minAcceptedType] to filter by. A circular reference is not an
 * error either, just a path that leads nowhere new — the answer to a yes/no question can't be made ambiguous by
 * how a data owner is reached, only by whether it is.
 *
 * [dataOwnerGroupId] is never reported as a member of itself, and a data owner that cannot be loaded is a dead end:
 * it declares no link, so no one is a member of [dataOwnerGroupId] through it.
 *
 * The data owners are read as they are stored, so a caller that has just changed some links sees the effect of that
 * change, and a link declared directly by a data owner of the batch counts exactly like a transitive one.
 *
 * @param dataOwnerIds the ids of the data owners whose membership is checked.
 * @param dataOwnerGroupId the id of the data owner representing the group the membership is checked against.
 * @param loadDataOwners loads the data owners with the provided ids, omitting the ids that do not match any
 * existing data owner.
 * @return the subset of [dataOwnerIds] that is a member of [dataOwnerGroupId].
 * @throws IllegalEntityException if a `dataOwnerGroups` entry with a blank id is found, or if answering requires
 * loading more than [MAX_HCP_ANCESTORS] data owners per entry of [dataOwnerIds] — the same safety limit
 * [resolveDataOwnerAncestors] puts on a single hierarchy, scaled to the size of the batch since the batch shares
 * one traversal.
 */
suspend fun <T> filterDataOwnersMembersOf(
	dataOwnerIds: Set<String>,
	dataOwnerGroupId: String,
	loadDataOwners: suspend (Set<String>) -> Collection<T>,
): Set<String> where T : CryptoActor, T : Identifiable<String> {
	val stillSearching = (dataOwnerIds - dataOwnerGroupId).toMutableSet()
	if (stillSearching.isEmpty()) return emptySet()
	val members = mutableSetOf<String>()
	val loadedById = mutableMapOf<String, T>()
	val requestedIds = mutableSetOf<String>()
	/*
	 * The data owners whose links still have to be followed, each with the members of the batch that reached it and
	 * are still looking for the group, plus, for every data owner ever reached, the members of the batch it has
	 * already been followed for. Following a data owner at most once per member of the batch is what makes a shared
	 * ancestor cost one load and one expansion however many members stand below it, and what keeps a circular
	 * reference from looping forever.
	 */
	var frontier: Map<String, Set<String>> = stillSearching.associateWith { setOf(it) }
	val followedFor = stillSearching.associateWithTo(mutableMapOf()) { mutableSetOf(it) }
	while (frontier.isNotEmpty() && stillSearching.isNotEmpty()) {
		val idsToLoad = frontier.keys - requestedIds
		if (idsToLoad.isNotEmpty()) {
			requestedIds += idsToLoad
			if (requestedIds.size > MAX_HCP_ANCESTORS * dataOwnerIds.size) {
				throw IllegalEntityException(
					"Too many data owners to follow to check the membership of ${dataOwnerIds.size} data owners to $dataOwnerGroupId:" +
						" exceeds the maximum of $MAX_HCP_ANCESTORS per checked data owner",
				)
			}
			loadDataOwners(idsToLoad).forEach { loadedById[it.id] = it }
		}
		val nextFrontier = mutableMapOf<String, MutableSet<String>>()
		frontier.forEach { (followedId, reachedBy) ->
			val followed = loadedById[followedId] ?: return@forEach
			val searchingBelow = reachedBy.filterTo(mutableSetOf()) { it in stillSearching }
			if (searchingBelow.isEmpty()) return@forEach
			val links = followed.declaredGroupLinkIds(followed)
			if (dataOwnerGroupId in links) {
				members += searchingBelow
				stillSearching -= searchingBelow
			} else {
				links.forEach { linkedId ->
					val alreadyFollowedFor = followedFor.getOrPut(linkedId) { mutableSetOf() }
					searchingBelow.forEach {
						if (alreadyFollowedFor.add(it)) nextFrontier.getOrPut(linkedId) { mutableSetOf() } += it
					}
				}
			}
		}
		frontier = nextFrontier
	}
	return members
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
private fun <T> T.declaredGroupLinkIds(childDataOwner: T): List<String> where T : CryptoActor, T : Identifiable<String> =
	(listOfNotNull(parentId?.takeIf { it.isNotBlank() }) + dataOwnerGroups.map { it.dataOwnerId }.onEach { groupId ->
		if (groupId.isBlank()) {
			throw IllegalEntityException("Blank group id for healthcare party ${childDataOwner.id}")
		}
	}).distinct()

/**
 * The groups this healthcare party is directly linked to (see [declaredGroupLinkIds]), together with the *effective*
 * type of each link — i.e. the linked group's own type ([org.taktik.icure.entities.base.CryptoActor.effectiveGroupLinkType]),
 * not anything declared by this healthcare party. Links to a group not present in [loadedById] (missing or not yet
 * loaded) are skipped, matching the "links to healthcare parties that cannot be loaded are ignored" tolerance.
 * Restricted to links whose target's effective type [isAtLeast] [minAcceptedType], if it is not null.
 */
private fun <T> T.groupLinksWithEffectiveType(
	childDataOwner: T,
	loadedById: Map<String, T>,
	minAcceptedType: DataOwnerGroupLinkType?,
	dataOwnerType: DataOwnerType,
): List<Pair<DataOwnerGroupLinkType, String>> where T : CryptoActor, T : Identifiable<String> = declaredGroupLinkIds(childDataOwner)
	.mapNotNull { groupId -> loadedById[groupId]?.let { target -> target.effectiveGroupLinkType(dataOwnerType) to groupId } }
	.filter { (linkType, _) -> linkType.isAtLeast(minAcceptedType) }

/**
 * Whether a link of this type may transitively follow a link of type [previous] on the same path away from the
 * starting healthcare party: only allowed if this link's [DataOwnerGroupLinkType.strength] is lower than
 * [previous]'s, or the same and of the same type (see [resolveHcpAncestors]).
 */
private fun DataOwnerGroupLinkType.canTransitivelyFollow(previous: DataOwnerGroupLinkType): Boolean = strength < previous.strength || this == previous
