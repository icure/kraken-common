package org.taktik.icure.services.external.rest.v2.dto.security.jwt

data class JwtResponseDto(
	val token: String? = null,
	val refreshToken: String? = null,
	val successful: Boolean = false
)