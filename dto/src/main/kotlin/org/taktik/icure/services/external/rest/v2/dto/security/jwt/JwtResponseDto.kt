package org.taktik.icure.services.external.rest.v2.dto.security.jwt

/**
 * Response returned by JWT-based authentication endpoints, containing the issued tokens upon successful login.
 */
data class JwtResponseDto(
	/** The short-lived JWT access token, or null if authentication was not successful. */
	val token: String? = null,
	/** The longer-lived refresh token used to obtain new access tokens, or null if not issued. */
	val refreshToken: String? = null,
	/** Whether the authentication attempt succeeded. */
	val successful: Boolean = false,
)
