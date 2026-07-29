package org.taktik.icure.entities.base

/**
 * The nature of the link between a crypto actor and a data owner representing one of its groups.
 *
 * ## Membership propagation
 * All links are transitive, whatever their type: every directly linked group is a group of the actor, and the groups
 * of a group are also groups of the actor (applied recursively). An actor is therefore a member of every group
 * reachable through a path of links.
 *
 * For example with `hcp -parent-> department -other-> building -parent-> campus`: the groups of `hcp` are
 * `department`, `building` and `campus`.
 *
 * ## Administrative rights
 * Only [parent] links define the administrative hierarchy: permissions on "child" data owners (conditional
 * create/update/delete of a child hcp, acting in the scope of a child data owner, ...) are granted exclusively
 * through the legacy `parentId` and [parent]-type links. [other] links only provide group membership for data
 * sharing purposes and never grant administrative rights over the linked data owner.
 */
enum class DataOwnerGroupLinkType {
	parent,
	other,
}
