package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema

data class GroupDeletionReportDto(
	@get:Schema(required = true) val type: String,
	val id: String,
	@get:Schema(required = true) val server: String,
)
