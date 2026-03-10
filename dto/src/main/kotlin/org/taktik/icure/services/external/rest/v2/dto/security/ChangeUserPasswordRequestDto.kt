package org.taktik.icure.services.external.rest.v2.dto.security

import io.swagger.v3.oas.annotations.media.Schema

data class ChangeUserPasswordRequestDto(
	@param:Schema(required = true)
	val newPassword: String,
)