package org.taktik.icure.security

import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.exceptions.IllegalEntityException

/**
 * Resolves the trees of groups (parents, organisations, locations, ...) that [childHcp] is a member of, following the
 * legacy [HealthcareParty.parentId] link plus all [HealthcareParty.dataOwnerGroups] links.
 *
 * Membership propagates through all links, whatever their type: the groups of a group joined through a link are
 * included in the result, recursively.
 *
 * A same group may appear multiple times in the result when it is reachable through different paths (diamond
 * configurations), but a circular reference along a single path causes an [IllegalEntityException]. Direct
 * self-references and links to healthcare parties that cannot be loaded are ignored.
 *
 * @param childHcp the healthcare party to resolve the group hierarchies of.
 * @param loadHealthcareParties loads the healthcare parties with the provided ids, omitting the ids that do not
 * match any existing healthcare party.
 * @return one tree for each group [childHcp] is directly linked to.
 * @throws IllegalEntityException if a link with a blank id or a circular reference is found.
 */
suspend fun resolveHcpHierarchies(
	childHcp: HealthcareParty,
	loadHealthcareParties: suspend (Set<String>) -> Collection<HealthcareParty>,
): List<HealthcarePartyWithHierarchy> {
	val loadedById = mutableMapOf(childHcp.id to childHcp)
	val expandedIds = mutableSetOf(childHcp.id)
	var frontier = listOf(childHcp)
	while (frontier.isNotEmpty()) {
		val links = frontier.flatMap { it.validatedGroupLinks(childHcp) }
		val idsToLoad = links.toSet() - loadedById.keys
		if (idsToLoad.isNotEmpty()) {
			loadHealthcareParties(idsToLoad).forEach { loadedById[it.id] = it }
		}
		frontier = links.filter { expandedIds.add(it) }.mapNotNull { loadedById[it] }
	}

	fun buildHierarchies(of: HealthcareParty, pathIds: Set<String>): List<HealthcarePartyWithHierarchy> = of.validatedGroupLinks(childHcp).mapNotNull { groupId ->
		when {
			groupId == of.id -> null // tolerated for compatibility with the legacy parentId handling
			groupId in pathIds -> throw IllegalEntityException(
				"Circular reference in the hcp hierarchy starting from ${childHcp.id} detected.",
			)
			else -> loadedById[groupId]?.let { group ->
				HealthcarePartyWithHierarchy(
					dataOwner = group,
					parents = buildHierarchies(group, pathIds + groupId),
				)
			}
		}
	}
	return buildHierarchies(childHcp, setOf(childHcp.id))
}

/**
 * The ids of the groups this healthcare party is directly linked to (legacy [HealthcareParty.parentId] plus
 * [HealthcareParty.dataOwnerGroups]), deduplicated.
 * @throws IllegalEntityException if any of the linked ids is blank.
 */
private fun HealthcareParty.validatedGroupLinks(childHcp: HealthcareParty): List<String> = (
	listOfNotNull(parentId) + dataOwnerGroups.map { it.dataOwnerId }
	)
	.onEach { groupId ->
		if (groupId.isBlank()) {
			throw IllegalEntityException("Blank parent id or group id for healthcare party ${childHcp.id}")
		}
	}
	.distinct()