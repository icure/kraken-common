package org.taktik.icure.entities.dao

data class RangeQueryParameters(
	val startKey: KeyComponent<*>,
	val endKey: KeyComponent<*>,
)
