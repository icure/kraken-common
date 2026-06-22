package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * DTO representing a report generated after a group deletion operation.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.GroupDeletionReportDto")
data class GroupDeletionReportDto(
	/** The type of the deleted resource. */
	@param:Schema(required = true) @ActiveField val type: String,
	/** The unique identifier of the deleted group. */
	@ActiveField val id: String,
	/** The server on which the group was deleted. */
	@param:Schema(required = true) @ActiveField val server: String,
)
