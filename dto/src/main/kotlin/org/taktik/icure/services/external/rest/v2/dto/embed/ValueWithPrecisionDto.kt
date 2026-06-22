package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a numeric value paired with its precision (number of significant digits).
 */
data class ValueWithPrecisionDto(
	/** The numeric value. */
	@ActiveField val value: Int,
	/** The number of significant digits. */
	@ActiveField val precision: Int,
)
