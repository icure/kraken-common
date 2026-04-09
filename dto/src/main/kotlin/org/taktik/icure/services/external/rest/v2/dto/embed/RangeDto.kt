package org.taktik.icure.services.external.rest.v2.dto.embed

/**
 * Represents a numeric range defined by a low and high bound.
 */
data class RangeDto(
	/** is the lower bound (inclusive) of the range */
	val low: Double? = null,
	/** is the higher bound (inclusive) of the range */
	val high: Double? = null,
)
