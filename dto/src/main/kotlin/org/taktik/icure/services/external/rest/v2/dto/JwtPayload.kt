package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO wrapping a JSON Web Token (JWT) string.
 */
data class JwtPayload(
	/** The encoded JWT string. */
	@param:Schema(required = true) val jwt: String,
)
