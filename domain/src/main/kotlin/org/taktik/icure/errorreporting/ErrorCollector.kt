package org.taktik.icure.errorreporting

/**
 * An interface for reporting warnings and errors.
 * This interface should be used only for reporting user errors and not server or system errors.
 * Different implementations may choose to throw exceptions, accumulate errors for later retrieval, or other.
 */
interface ErrorCollector {
	/**
	 * Report a non-fatal error.
	 * When using an error collector for entity validation, validation will generally continue, to collect other
	 * warnings/errors, and the entity is generally considered valid, but warnings could point to potential issues and
	 * ignoring them could cause unexpected/unintuitive behaviors.
	 * Parameters might be converted using [Any.toString] in a second moment when creating error messages.
	 * For mutable values, you should convert the parameter to string before passing it to this method.
	 */
	fun addWarning(code: String, params: Map<String, Any> = emptyMap())
	/**
	 * Report a fatal error.
	 * When using an error collector for entity validation, depending on the implementation, validation may continue, to
	 * collect other warnings/errors, but the entity is considered invalid and in general can't be used for the intended
	 * purpose.
	 * Parameters might be converted using [Any.toString] in a second moment when creating error messages.
	 * For mutable values, you should convert the parameter to string before passing it to this method.
	 */
	fun addError(code: String, params: Map<String, Any> = emptyMap())

	/**
	 * An error collector that throws an exception on errors and ignores warnings.
	 */
	object Throwing : ErrorCollector {
		override fun addWarning(code: String, params: Map<String, Any>) {
			// do nothing
		}

		override fun addError(code: String, params: Map<String, Any>): Nothing {
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

		override fun addWarning(code: String, params: Map<String, Any>) {
			warningList.add(CodedErrorDetails(code, params))
		}

		override fun addError(code: String, params: Map<String, Any>) {
			errorList.add(CodedErrorDetails(code, params))
		}
	}
}

fun ErrorCollector.addError(code: String, vararg params: Pair<String, Any>) {
	this.addError(code, mapOf(*params))
}

fun ErrorCollector.addWarning(code: String, vararg params: Pair<String, Any>) {
	this.addWarning(code, mapOf(*params))
}