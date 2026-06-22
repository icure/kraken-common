package org.taktik.icure.services.external.rest.v2.dto.security.jwt
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Response returned by JWT-based authentication endpoints, containing the issued tokens upon successful login.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.security.jwt.JwtResponseDto")
data class JwtResponseDto(
	/** The short-lived JWT access token, or null if authentication was not successful. */
	@ActiveField val token: String? = null,
	/** The longer-lived refresh token used to obtain new access tokens, or null if not issued. */
	@ActiveField val refreshToken: String? = null,
	/** Whether the authentication attempt succeeded. */
	@ActiveField val successful: Boolean = false,
)
