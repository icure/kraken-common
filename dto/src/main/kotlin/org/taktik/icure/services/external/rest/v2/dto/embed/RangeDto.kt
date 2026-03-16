package org.taktik.icure.services.external.rest.v2.dto.embed

/**
 * Represents a numeric range defined by a low and high bound.
 */
data class RangeDto(
	/** The lower bound of the range. */
	val low: Double? = null,
	/** The upper bound of the range. */
	val high: Double? = null,
)
