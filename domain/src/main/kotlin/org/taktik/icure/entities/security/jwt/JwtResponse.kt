package org.taktik.icure.entities.security.jwt

data class JwtResponse(
	/** The short-lived JWT access token, or null if authentication was not successful. */
	val token: String? = null,
	/** The longer-lived refresh token used to obtain new access tokens, or null if not issued. */
	val refreshToken: String? = null,
	/** Whether the authentication attempt succeeded. */
	val successful: Boolean = false,
)
