package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Simple wrapper DTO containing a single boolean response value.
 */
data class BooleanResponseDto(
	/** The boolean result of the operation. */
	@param:Schema(required = true) val response: Boolean,
)
