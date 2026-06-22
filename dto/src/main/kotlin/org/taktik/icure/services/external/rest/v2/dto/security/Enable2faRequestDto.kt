package org.taktik.icure.services.external.rest.v2.dto.security

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Request payload for enabling two-factor authentication (2FA) for a user account.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.security.Enable2faRequestDto")
data class Enable2faRequestDto(
	/** The shared TOTP secret used to generate one-time passwords. */
	@param:Schema(required = true)
	@ActiveField val secret: String,
	/** The number of digits in each generated one-time password. */
	@ActiveField val otpLength: Int,
	/**
	 * The otp at the current time for the provided configuration
	 */
	@ActiveField val otp: String,
	/**
	 * If null defaults to SHA1 as many authenticator apps still do not support different algorithms
	 */
	@ActiveField val algorithm: Algorithm? = null,
) {
	enum class Algorithm {
		SHA1, SHA256, SHA512
	}
}
