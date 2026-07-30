package org.taktik.icure.security

import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.base.DataOwnerIdWithHierarchy
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
 * Every [DataOwnerGroupLinkType] has a [DataOwnerGroupLinkType.strength]. Along any single path away from [childHcp],
 * the strength of successive links may only stay the same (with the same link type) or decrease: a link stronger
 * than the one before it, or of the same strength but a different type ("shifting"), makes the transitive link
 * ambiguous and is rejected. This is intentionally conservative — a single ambiguous link fails the whole
 * resolution, even if the same target is also reachable through another, unambiguous path (diamond configurations)
 * — since there is no current use case for the more permissive alternative (e.g. resolving the transitive type from
 * the least-restrictive valid path). This restriction may be relaxed in the future.
 *
 * A group reachable through different paths (diamond configurations) is legal and appears exactly once, but a
 * circular reference along a single path causes an [IllegalEntityException]. Direct self-references and links to
 * healthcare parties that cannot be loaded are ignored.
 *
 * @param childHcp the healthcare party to resolve the ancestor groups of.
 * @param loadHealthcareParties loads the healthcare parties with the provided ids, omitting the ids that do not
 * match any existing healthcare party.
 * @param restrictToLinksOfType if not null, only links whose type is included in this set are followed; all other
 * links are ignored as if they were not present.
 * @return the ancestor groups of [childHcp], deduplicated, excluding [childHcp] itself, in depth-first
 * first-encounter order following the declaration order of the links.
 * @throws IllegalEntityException if a link with a blank id or a circular reference is found, if the number of
 * distinct ancestor groups exceeds [MAX_HCP_ANCESTORS], or if a link stronger than (or of the same strength as, but
 * a different type than) the previous link on the same path is found.
 */
suspend fun resolveHcpAncestors(
	childHcp: HealthcareParty,
	restrictToLinksOfType: Set<DataOwnerGroupLinkType>? = null,
	loadHealthcareParties: suspend (Set<String>) -> Collection<HealthcareParty>,
): List<HealthcareParty> {
	val loadedById = mutableMapOf(childHcp.id to childHcp)
	val expandedIds = mutableSetOf(childHcp.id)
	var frontier = listOf(childHcp)
	while (frontier.isNotEmpty()) {
		val links = frontier.flatMap { it.validatedGroupLinks(childHcp, restrictToLinksOfType) }
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
		of.validatedGroupLinksWithType(childHcp, restrictToLinksOfType).forEach { (linkType, groupId) ->
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
 * @throws IllegalEntityException if a link with a blank id or a circular reference is found, or if the number of
 * distinct ancestor groups exceeds [MAX_HCP_ANCESTORS].
 */
suspend fun resolveHcpAncestorIdsByRights(
	childHcp: HealthcareParty,
	loadHealthcareParties: suspend (Set<String>) -> Collection<HealthcareParty>,
): HcpAncestorIdsByRights {
	val simpleLinkedIds = resolveHcpAncestors(childHcp, null, loadHealthcareParties)
	val loadedById = simpleLinkedIds.associateBy { it.id }
	val parentLinkedIds = resolveHcpAncestors(childHcp, setOf(DataOwnerGroupLinkType.parent)) { ids ->
		ids.mapNotNull { loadedById[it] }
	}.mapTo(LinkedHashSet()) { it.id }
	return HcpAncestorIdsByRights(
		parentLinkedIds = parentLinkedIds,
		simpleLinkedIds = simpleLinkedIds.mapNotNullTo(LinkedHashSet()) { ancestor -> ancestor.id.takeIf { it !in parentLinkedIds } },
	)
}

/**
 * Same traversal as [resolveHcpAncestors] but returns the hierarchies of [childHcp] as a tree of ids rooted at
 * [childHcp] itself: the parents of each node are the groups it is directly linked to. Contrary to
 * [resolveHcpAncestors], a group reachable through multiple paths (diamond configurations) appears once per path.
 *
 * @param childHcp the healthcare party to resolve the id hierarchies of.
 * @param loadHealthcareParties loads the healthcare parties with the provided ids, omitting the ids that do not
 * match any existing healthcare party.
 * @param restrictToLinksOfType if not null, only links whose type is included in this set are followed; all other
 * links are ignored as if they were not present.
 * @return the id hierarchy tree rooted at [childHcp].
 * @throws IllegalEntityException if a link with a blank id or a circular reference is found, or if the number of
 * distinct ancestor groups exceeds [MAX_HCP_ANCESTORS].
 */
suspend fun resolveHcpHierarchyIds(
	childHcp: HealthcareParty,
	restrictToLinksOfType: Set<DataOwnerGroupLinkType>? = null,
	loadHealthcareParties: suspend (Set<String>) -> Collection<HealthcareParty>,
): DataOwnerIdWithHierarchy {
	// Loads all the ancestors and validates the links (no cycle: the recursion below terminates)
	val ancestorsById = resolveHcpAncestors(childHcp, restrictToLinksOfType, loadHealthcareParties).associateBy { it.id }
	fun nodeOf(hcp: HealthcareParty): DataOwnerIdWithHierarchy = DataOwnerIdWithHierarchy(
		id = hcp.id,
		parents = hcp.validatedGroupLinks(childHcp, restrictToLinksOfType)
			.filter { it != hcp.id }
			.mapNotNull { ancestorsById[it] }
			.map { nodeOf(it) },
	)
	return nodeOf(childHcp)
}

/**
 * The groups this healthcare party is directly linked to (legacy [HealthcareParty.parentId], treated as a
 * [DataOwnerGroupLinkType.parent] link, plus [HealthcareParty.dataOwnerGroups]), with their link type, deduplicated
 * by group id (the legacy [HealthcareParty.parentId] wins over a [HealthcareParty.dataOwnerGroups] entry for the same
 * id), restricted to links whose type is included in [restrictToLinksOfType] if it is not null.
 * @throws IllegalEntityException if any of the linked ids is blank.
 */
private fun HealthcareParty.validatedGroupLinksWithType(
	childHcp: HealthcareParty,
	restrictToLinksOfType: Set<DataOwnerGroupLinkType>?,
): List<Pair<DataOwnerGroupLinkType, String>> = (
	listOfNotNull(parentId?.let { DataOwnerGroupLinkType.parent to it }) +
		dataOwnerGroups.map { it.linkType to it.dataOwnerId }
	)
	.onEach { (_, groupId) ->
		if (groupId.isBlank()) {
			throw IllegalEntityException("Blank parent id or group id for healthcare party ${childHcp.id}")
		}
	}
	.filter { (linkType, _) -> restrictToLinksOfType == null || linkType in restrictToLinksOfType }
	.distinctBy { it.second }

/**
 * The ids of the groups this healthcare party is directly linked to (see [validatedGroupLinksWithType]).
 * @throws IllegalEntityException if any of the linked ids is blank.
 */
private fun HealthcareParty.validatedGroupLinks(
	childHcp: HealthcareParty,
	restrictToLinksOfType: Set<DataOwnerGroupLinkType>?,
): List<String> = validatedGroupLinksWithType(childHcp, restrictToLinksOfType).map { it.second }

/**
 * Whether a link of this type may transitively follow a link of type [previous] on the same path away from the
 * starting healthcare party: only allowed if this link's [DataOwnerGroupLinkType.strength] is lower than
 * [previous]'s, or the same and of the same type (see [resolveHcpAncestors]).
 */
private fun DataOwnerGroupLinkType.canTransitivelyFollow(previous: DataOwnerGroupLinkType): Boolean = strength < previous.strength || this == previous
