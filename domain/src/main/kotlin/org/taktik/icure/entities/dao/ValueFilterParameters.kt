package org.taktik.icure.entities.dao

data class ValueFilterParameters(
	/**
	 * If not null, represents the key in the value object to check for filtering. If null, it means that the value is a scalar.
	 */
	val valueKey: String?,
	val range: RangeQueryParameters
)