package org.taktik.icure.errorreporting

data class CollectedErrors(
	val warnings: List<CodedErrorDetails>,
	val errors: List<CodedErrorDetails>,
)