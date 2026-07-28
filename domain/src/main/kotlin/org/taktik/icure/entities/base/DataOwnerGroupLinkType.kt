package org.taktik.icure.entities.base

/**
 * The nature of the link between a crypto actor and a data owner representing one of its groups.
 *
 * ## Membership propagation
 * An actor is a member of every group reachable through a path made of transitive links, optionally ended by a single
 * non-transitive link. In other words:
 * - every directly linked group is a group of the actor, whatever the link type;
 * - the groups of a group joined through a transitive link are also groups of the actor (applied recursively);
 * - a group joined through a non-transitive link is a group of the actor, but its own groups are not: a
 *   non-transitive link found at the end of a transitive path MUST still be followed (its destination is included),
 *   and nothing is included beyond it.
 *
 * For example with `hcp -parent-> department -location-> building -parent-> campus`: the groups of `hcp` are
 * `department` and `building` (reached through the non-transitive `location` link of `department`), but not `campus`.
 *
 * ## Administrative rights
 * Only [parent] links define the administrative hierarchy: permissions on "child" data owners (conditional
 * create/update/delete of a child hcp, acting in the scope of a child data owner, ...) are granted exclusively
 * through the legacy `parentId` and [parent]-type links. [organisation] and [location] links only provide group
 * membership for data sharing purposes and never grant administrative rights over the linked data owner.
 *
 * @property transitive whether group membership propagates past the group this kind of link points at (see above).
 */
enum class DataOwnerGroupLinkType(val transitive: Boolean) {
	parent(true),
	organisation(true),
	location(false),
}
