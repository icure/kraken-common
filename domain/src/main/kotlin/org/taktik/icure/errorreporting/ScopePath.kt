package org.taktik.icure.errorreporting

/**
 * A mutable path representing the current scope in which some operation is happening.
 * Can be used to provide context in error reporting.
 *
 * Builds a string representing the current scope: entering a scope appends a string to the path, exiting a scope
 * removes the last appended string(s).
 *
 * [toString] gives the concatenation of all currently entered scopes.
 */
interface ScopePath {
	companion object {
		operator fun invoke(vararg pathSegments: Any): ScopePath =
			ScopePathImpl(arrayListOf(*pathSegments))
	}

	fun enterScope(scope: Any)
	fun enterScope(vararg scope: Any)
	fun exitScope()
	fun exitScopes(depth: Int)
}

@JvmInline
private value class ScopePathImpl(private val pathSegments: ArrayList<Any>) : ScopePath {

	override fun toString(): String = buildString { pathSegments.forEach { append(it) } }

	override fun enterScope(scope: Any) {
		pathSegments.addLast(scope)
	}

	override fun enterScope(vararg scope: Any) {
		pathSegments.addAll(scope.asList())
	}

	override fun exitScope() {
		pathSegments.removeLast()
	}

	override fun exitScopes(depth: Int) {
		repeat(depth) {
			pathSegments.removeLast()
		}
	}
}

/**
 * Appends the given string to the scope path for the duration of the [block], then removes it.
 */
inline fun <T> ScopePath.appending(toAppend: Any, block: () -> T): T {
	enterScope(toAppend)
	return try {
		block()
	} finally {
		exitScope()
	}
}

/**
 * Appends the given strings to the scope path for the duration of the [block], then removes it.
 */
inline fun <T> ScopePath.appending(vararg toAppend: Any, block: () -> T): T {
	enterScope(*toAppend)
	return try {
		block()
	} finally {
		exitScopes(toAppend.size)
	}
}