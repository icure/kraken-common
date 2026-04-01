package org.taktik.icure.entities.embed

/**
 * A general range of values.
 *
 * @property low is the lower bound (inclusive) of the range
 * @property high is the higher bound (inclusive) of the range
 */
data class Range(
	val low: Double? = null,
	val high: Double? = null,
)
