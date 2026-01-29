package org.taktik.icure.errorreporting

/**
 * Details of a coded user error, used to transfer language-independent error information without throwing exceptions.
 */
data class CodedErrorDetails(
	val code: String,
	val params: Map<String, Any>
)
