package org.taktik.icure.entities.base

/**
 * The id of a data owner together with its group hierarchies (see [DataOwnerGroupLinkType] for the membership
 * propagation rules) as a tree of ids.
 */
data class DataOwnerIdWithHierarchy(
	/** The id of the data owner. */
	val id: String,
	/**
	 * One node for each group the data owner is directly linked to, through the legacy parentId or a dataOwnerGroups
	 * link, in the declaration order of the links. A group reachable through multiple paths appears once per path.
	 */
	val parents: List<DataOwnerIdWithHierarchy>,
)
