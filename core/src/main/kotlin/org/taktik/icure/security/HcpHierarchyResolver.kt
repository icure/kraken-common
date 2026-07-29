package org.taktik.icure.security

import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.base.DataOwnerIdWithHierarchy
import org.taktik.icure.exceptions.IllegalEntityException

/**
 * Resolves all the (transitive) ancestor groups (parents, organisations, locations, ...) that [childHcp] is a member
 * of, following the legacy [HealthcareParty.parentId] link plus all [HealthcareParty.dataOwnerGroups] links.
 *
 * Membership propagates through all links, whatever their type: the groups of a group joined through a link are
 * included in the result, recursively.
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
 * @throws IllegalEntityException if a link with a blank id or a circular reference is found.
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
	}

	val ancestors = LinkedHashMap<String, HealthcareParty>()
	fun visit(of: HealthcareParty, pathIds: Set<String>) {
		of.validatedGroupLinks(childHcp, restrictToLinksOfType).forEach { groupId ->
			when {
				groupId == of.id -> {} // tolerated for compatibility with the legacy parentId handling
				groupId in pathIds -> throw IllegalEntityException(
					"Circular reference in the hcp hierarchy starting from ${childHcp.id} detected.",
				)
				else -> loadedById[groupId]?.let { group ->
					if (ancestors.putIfAbsent(group.id, group) == null) {
						visit(group, pathIds + groupId)
					}
				}
			}
		}
	}
	visit(childHcp, setOf(childHcp.id))
	return ancestors.values.toList()
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
 * @throws IllegalEntityException if a link with a blank id or a circular reference is found.
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
 * The ids of the groups this healthcare party is directly linked to (legacy [HealthcareParty.parentId], treated as a
 * [DataOwnerGroupLinkType.parent] link, plus [HealthcareParty.dataOwnerGroups]), deduplicated, restricted to links
 * whose type is included in [restrictToLinksOfType] if it is not null.
 * @throws IllegalEntityException if any of the linked ids is blank.
 */
private fun HealthcareParty.validatedGroupLinks(
	childHcp: HealthcareParty,
	restrictToLinksOfType: Set<DataOwnerGroupLinkType>?,
): List<String> = (
	listOfNotNull(parentId?.let { DataOwnerGroupLinkType.parent to it }) +
		dataOwnerGroups.map { it.linkType to it.dataOwnerId }
	)
	.onEach { (_, groupId) ->
		if (groupId.isBlank()) {
			throw IllegalEntityException("Blank parent id or group id for healthcare party ${childHcp.id}")
		}
	}
	.filter { (linkType, _) -> restrictToLinksOfType == null || linkType in restrictToLinksOfType }
	.map { it.second }
	.distinct()
