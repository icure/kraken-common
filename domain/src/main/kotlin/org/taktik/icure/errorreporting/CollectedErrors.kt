package org.taktik.icure.errorreporting

data class CollectedErrors(
	val warnings: List<String>,
	val errors: List<String>,
)