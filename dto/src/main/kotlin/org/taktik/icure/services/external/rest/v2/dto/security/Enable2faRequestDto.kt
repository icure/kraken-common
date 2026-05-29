package org.taktik.icure.services.external.rest.v2.dto.security

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Request payload for enabling two-factor authentication (2FA) for a user account.
 */
data class Enable2faRequestDto(
	/** The shared TOTP secret used to generate one-time passwords. */
	@param:Schema(required = true)
	val secret: String,
	/** The number of digits in each generated one-time password. */
	val otpLength: Int,
	/**
	 * The otp at the current time for the provided configuration
	 */
	val otp: String,
	/**
	 * If null defaults to SHA1 as many authenticator apps still do not support different algorithms
	 */
	val algorithm: Algorithm? = null,
) {
	enum class Algorithm {
		SHA1, SHA256, SHA512
	}
}
