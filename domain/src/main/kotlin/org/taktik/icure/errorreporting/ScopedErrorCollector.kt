package org.taktik.icure.errorreporting

/**
 * An [ErrorCollector] that adds scope path information to each reported error or warning.
 * The [pathCollector] is optional, if not available a path parameter will still be provided to the underlying error
 * collector, but the value will be a constant string.
 */
class ScopedErrorCollector(
	private val errorCollector: ErrorCollector,
	private val pathCollector: ScopePath?
) : ErrorCollector, ScopePath {
	companion object {
		const val PATH_PARAM_NAME = "path"
		private const val NO_PATH_VALUE = "<unknown path>"
	}

	override fun addWarning(errorMessage: String) {
		errorCollector.addWarning("${pathCollector?.toString() ?: NO_PATH_VALUE}: $errorMessage")
	}

	override fun addError(errorMessage: String) {
		errorCollector.addError("${pathCollector?.toString() ?: NO_PATH_VALUE}: $errorMessage")
	}

//	private fun paramsWithPath(params: Map<String, String>): Map<String, String> =
//		params + (PATH_PARAM_NAME to (pathCollector?.toString() ?: NO_PATH_VALUE))

//	override fun addWarning(code: String, params: Map<String, String>) {
//		errorCollector.addWarning(code, paramsWithPath(params))
//	}
//
//	override fun addError(code: String, params: Map<String, String>) {
//		errorCollector.addError(code, paramsWithPath(params))
//	}

	override fun enterScope(scope: Any) {
		pathCollector?.enterScope(scope)
	}

	override fun enterScope(vararg scope: Any) {
		pathCollector?.enterScope(*scope)
	}

	override fun exitScope() {
		pathCollector?.exitScope()
	}

	override fun exitScopes(depth: Int) {
		pathCollector?.exitScopes(depth)
	}
}