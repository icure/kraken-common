package org.taktik.icure.services.external.rest.v2.dto.security

import io.swagger.v3.oas.annotations.media.Schema

data class Enable2faRequestDto(
	@Schema(required = true)
	val secret: String,
	val otpLength: Int,
)
