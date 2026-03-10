package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a numeric value paired with its precision (number of significant digits).
 */
data class ValueWithPrecisionDto(
	/** The numeric value. */
	val value: Int,
	/** The number of significant digits. */
	val precision: Int,
)
