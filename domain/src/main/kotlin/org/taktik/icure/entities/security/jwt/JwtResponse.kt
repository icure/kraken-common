package org.taktik.icure.entities.security.jwt

data class JwtResponse(
	val token: String? = null,
	val refreshToken: String? = null,
	val successful: Boolean = false,
)
