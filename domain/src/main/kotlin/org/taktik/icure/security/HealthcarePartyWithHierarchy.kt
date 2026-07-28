package org.taktik.icure.security

import org.taktik.icure.entities.HealthcareParty
import java.io.Serializable

/**
 * A node of a data owner group hierarchy identified only by its id: [parents] are the groups the data owner with [id]
 * is a member of (empty if the data owner is not part of any group, or if membership does not propagate past it).
 *
 * Note: this class is part of the session-stored user details, so it must stay [Serializable].
 */
data class IdWithHierarchy(
	val id: String,
	val parents: List<IdWithHierarchy> = emptyList(),
) : Serializable {
	companion object {
		private const val serialVersionUID = 1L
	}
}

/**
 * Whether [id] is the id of any node reachable from [this] level, walking up through parents one level at a time.
 */
tailrec fun List<IdWithHierarchy>.containsId(id: String): Boolean {
	if (isEmpty()) return false
	if (any { it.id == id }) return true
	return flatMap { it.parents }.containsId(id)
}

/**
 * A node of a data owner group hierarchy holding the full [HealthcareParty]: [parents] are the groups [dataOwner] is
 * a member of (empty if the data owner is not part of any group, or if membership does not propagate past it).
 */
data class HealthcarePartyWithHierarchy(
	val dataOwner: HealthcareParty,
	val parents: List<HealthcarePartyWithHierarchy> = emptyList(),
)

fun HealthcarePartyWithHierarchy.toHierarchyNode(): IdWithHierarchy = IdWithHierarchy(dataOwner.id, parents.map { it.toHierarchyNode() })

fun HealthcarePartyWithHierarchy.any(predicate: (HealthcareParty) -> Boolean): Boolean {
	if (predicate(dataOwner)) return true
	return parents.any { it.any(predicate) }
}

/**
 * All the healthcare parties in this hierarchy (this node and all of its ancestors, including duplicates if a same
 * healthcare party is reachable through multiple paths).
 */
fun HealthcarePartyWithHierarchy.allHealthcareParties(): List<HealthcareParty> = listOf(dataOwner) + parents.flatMap { it.allHealthcareParties() }
