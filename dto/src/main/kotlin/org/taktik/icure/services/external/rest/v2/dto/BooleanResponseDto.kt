package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema

data class BooleanResponseDto(
	@get:Schema(required = true) val response: Boolean,
)
