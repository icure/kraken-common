package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema

data class GroupDeletionReportDto(
	@param:Schema(required = true) val type: String,
	val id: String,
	@param:Schema(required = true) val server: String,
)
