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
)
