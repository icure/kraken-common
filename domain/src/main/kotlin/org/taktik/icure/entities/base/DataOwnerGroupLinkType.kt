package org.taktik.icure.entities.base

/**
 * The nature of the link between a crypto actor and a data owner representing one of its groups.
 *
 * @property transitive whether group membership propagates through this kind of link. When a link is transitive and the
 * linked group is itself a member of another group through a transitive link, the original actor is a member of that
 * further group as well, so resolving the complete set of groups of an actor requires following transitive links
 * recursively. A non-transitive link only ever grants membership of the group it points at.
 */
enum class DataOwnerGroupLinkType(val transitive: Boolean) {
	parent(true),
	organisation(true),
	location(false),
}
