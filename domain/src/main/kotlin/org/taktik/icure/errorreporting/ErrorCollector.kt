package org.taktik.icure.errorreporting

/**
 * An interface for reporting warnings and errors.
 * This interface should be used only for reporting user errors and not server or system errors.
 * Different implementations may choose to throw exceptions, accumulate errors for later retrieval, or other.
 */
interface ErrorCollector {
	/**
	 * Report a non-fatal error, which could hide a potential issue, but can be ignored.
	 */
	fun addWarning(code: String, params: Map<String, String>/* = emptyMap() */)
	/**
	 * Report a fatal error, which prevents further processing.
	 */
	fun addError(code: String, params: Map<String, String>/* = emptyMap() */)

	/**
	 * An error collector that throws an exception on errors and ignores warnings.
	 */
	object Throwing : ErrorCollector {
		override fun addWarning(code: String, params: Map<String, String>) {
			// do nothing
		}

		override fun addError(code: String, params: Map<String, String>): Nothing {
			throw CodedErrorException(code, params)
		}
	}

	/**
	 * An error collector that collects warnings and errors, without failing.
	 */
	class Collecting : ErrorCollector {
		private val warningList = mutableListOf<CodedErrorDetails>()
		private val errorList = mutableListOf<CodedErrorDetails>()

		/**
		 * All warning and errors collected so far.
		 */
		val collectedErrors: CollectedErrors = CollectedErrors(
			warnings = warningList,
			errors = errorList
		)

		override fun addWarning(code: String, params: Map<String, String>) {
			warningList.add(CodedErrorDetails(code, params))
		}

		override fun addError(code: String, params: Map<String, String>) {
			errorList.add(CodedErrorDetails(code, params))
		}
	}
}

fun ErrorCollector.addError(code: String, param1: Pair<String, String>, vararg params: Pair<String, String>) {
	this.addError(code, mapOf(param1, *params))
}

fun ErrorCollector.addWarning(code: String, param1: Pair<String, String>, vararg params: Pair<String, String>) {
	this.addWarning(code, mapOf(param1, *params))
}