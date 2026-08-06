package org.taktik.icure.services.external.rest.v2.dto.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.DataOwnerTypeDto

/**
 * The id of a data owner together with its group hierarchies as a tree of ids.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DataOwnerHierarchyInfoDto(
	/** The id of the data owner. */
	@param:Schema(required = true) @ActiveField val id: String,
	@ActiveField val dataOwnerType: DataOwnerTypeDto,
	/**
	 * One node for each group the data owner is directly linked to, through the legacy parentId or a dataOwnerGroups
	 * link, in the declaration order of the links. A group reachable through multiple paths appears in each path.
	 */
	@ActiveField val links: List<HierarchyNode> = emptyList(),
) {
	data class HierarchyNode(
		@ActiveField val linkedGroupId: String,
		@ActiveField val linkType: DataOwnerGroupLinkTypeDto,
		@ActiveField val transitiveLinks: List<HierarchyNode> = emptyList(),
	)
}
