package org.taktik.icure.errorreporting

/**
 * An [ErrorCollector] that adds scope path information to each reported error or warning.
 * The [path] is optional, if not available a path parameter will still be provided to the underlying error
 * collector, but the value will be a constant string.
 * The underlying error collector ([pathless]) must not be another [ScopedErrorCollector], as it would override the path
 * information instead of adding additional info.
 * You can access directly the underlying [pathless] error collector to report errors without adding path information.
 */
class ScopedErrorCollector(
	val pathless: ErrorCollector,
	val path: ScopePath?
) : ErrorCollector {
	init {
		check (pathless !is ScopedErrorCollector) {
			"Nested ScopedErrorCollector are not allowed"
		}
	}

	companion object {
		const val PATH_PARAM_NAME = "path"
		const val NO_PATH_VALUE = "<unknown path>"
	}

	private fun paramsWithPath(params: Map<String, Any>): Map<String, Any> =
		// NOTE: must do path.toString() NOW, since path is mutable
		params + (PATH_PARAM_NAME to (path?.toString() ?: NO_PATH_VALUE))

	override fun addWarning(code: String, params: Map<String, Any>) {
		pathless.addWarning(code, paramsWithPath(params))
	}

	override fun addError(code: String, params: Map<String, Any>) {
		pathless.addError(code, paramsWithPath(params))
	}
}

/**
 * Appends the given string to the scope path for the duration of the [block], then removes it.
 * Execute the block even if the receiver is null, but without appending anything to the path in that case.
 */
inline fun <T> ScopedErrorCollector?.appending(toAppend: Any, block: () -> T): T =
	this?.path.appending(toAppend, block)

// 2 and 3 args versions are very common, should be a bit faster than using vararg at least for ScopePathImpl
inline fun <T> ScopedErrorCollector?.appending(toAppend1: Any, toAppend2: Any, block: () -> T): T =
	this?.path.appending(toAppend1, toAppend2, block)

// 2 and 3 args versions are very common, should be a bit faster than using vararg at least for ScopePathImpl
inline fun <T> ScopedErrorCollector?.appending(toAppend1: Any, toAppend2: Any, toAppend3: Any, block: () -> T): T =
	this?.path.appending(toAppend1, toAppend2, toAppend3, block)

/**
 * Appends the given strings to the scope path for the duration of the [block], then removes it.
 * Execute the block even if the receiver is null, but without appending anything to the path in that case.
 */
inline fun <T> ScopedErrorCollector?.appending(vararg toAppend: Any, block: () -> T): T =
	this?.path.appending(*toAppend, block = block)