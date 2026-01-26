package org.taktik.icure.errorreporting

interface ErrorCollector {
	fun addWarning(errorMessage: String)
	fun addError(errorMessage: String)
//	fun addWarning(code: String, params: Map<String, String>)
//	fun addError(code: String, params: Map<String, String>)

	/**
	 * An error collector that throws an exception on errors and ignores warnings.
	 */
	object Throwing : ErrorCollector {
		override fun addWarning(errorMessage: String) {
			// do nothing
		}

		override fun addError(errorMessage: String): Nothing {
			throw IllegalArgumentException(errorMessage)
		}
	}

	/**
	 * An error collector that collects warnings and errors, without failing.
	 */
	class Collecting : ErrorCollector {
		private val warningList = mutableListOf<String>()
		private val errorList = mutableListOf<String>()

		/**
		 * All warning and errors collected so far.
		 */
		val collectedErrors: CollectedErrors = CollectedErrors(
			warnings = warningList,
			errors = errorList
		)

		override fun addWarning(errorMessage: String) {
			warningList.add(errorMessage)
		}

		override fun addError(errorMessage: String) {
			errorList.add(errorMessage)
		}
	}
}