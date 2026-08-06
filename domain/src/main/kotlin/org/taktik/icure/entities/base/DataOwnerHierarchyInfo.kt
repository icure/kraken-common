package org.taktik.icure.entities.base

import org.taktik.icure.entities.DataOwnerType

/**
 * The id and type of a data owner together with its group hierarchies (see [DataOwnerGroupLinkType] for the
 * membership propagation rules) as a tree of ids.
 *
 * Every data owner in the tree is of the same [dataOwnerType] as the root: this is not (currently) an enforced
 * invariant of this type itself, but a structural guarantee of the domain model, since a [CryptoActor]'s
 * `dataOwnerGroups` can currently only target healthcare parties.
 */
data class DataOwnerHierarchyInfo(
	/** The id of the data owner. */
	val id: String,
	/** The type of the data owner, and of every data owner reachable through [links]. */
	val dataOwnerType: DataOwnerType,
	/**
	 * One node for each group the data owner is directly linked to, through the legacy parentId or a dataOwnerGroups
	 * link, in the declaration order of the links. A group reachable through multiple paths appears once per path.
	 */
	val links: List<HierarchyNode> = emptyList(),
) {
	/**
	 * A single link to a group, together with the further links reachable transitively from that group.
	 *
	 * @property linkedGroupId the id of the linked group.
	 * @property linkType the type of this specific link (see [DataOwnerGroupLinkType]).
	 * @property transitiveLinks the links of the linked group itself. Per [DataOwnerGroupLinkType.strength], a link
	 * nested here may only have the same or a lower strength than [linkType] (a [DataOwnerGroupLinkType.parent] link
	 * may transitively weaken to [DataOwnerGroupLinkType.simple], never the other way around) — this is enforced
	 * upstream, when the hierarchy is resolved, not by this type itself.
	 */
	data class HierarchyNode(
		val linkedGroupId: String,
		val linkType: DataOwnerGroupLinkType,
		val transitiveLinks: List<HierarchyNode> = emptyList(),
	)
}
