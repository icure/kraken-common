package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema

data class GroupDeletionReportDto(
	@Schema(required = true) val type: String,
	val id: String,
	@Schema(required = true) val server: String
)