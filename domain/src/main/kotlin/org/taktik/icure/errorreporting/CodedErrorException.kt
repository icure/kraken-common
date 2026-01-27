package org.taktik.icure.errorreporting

/**
 * Language independent details of a coded user error represented as an exception.
 */
data class CodedErrorException(
	val code: String,
	val params: Map<String, String>
) : Exception("Error $code")
