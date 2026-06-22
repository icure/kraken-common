package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Simple wrapper DTO containing a single boolean response value.
 */
data class BooleanResponseDto(
	/** The boolean result of the operation. */
	@param:Schema(required = true) @ActiveField val response: Boolean,
)
