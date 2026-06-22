package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * DTO wrapping a JSON Web Token (JWT) string.
 */
data class JwtPayload(
	/** The encoded JWT string. */
	@param:Schema(required = true) @ActiveField val jwt: String,
)
