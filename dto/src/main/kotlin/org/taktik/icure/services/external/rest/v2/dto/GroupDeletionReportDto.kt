package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO representing a report generated after a group deletion operation.
 */
data class GroupDeletionReportDto(
	/** The type of the deleted resource. */
	@param:Schema(required = true) val type: String,
	/** The unique identifier of the deleted group. */
	val id: String,
	/** The server on which the group was deleted. */
	@param:Schema(required = true) val server: String,
)
