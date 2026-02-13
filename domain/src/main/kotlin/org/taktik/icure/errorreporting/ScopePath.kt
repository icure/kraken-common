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
	fun enterScopes(vararg scope: Any)
	fun exitScope()
	fun exitScopes(depth: Int)
}

@JvmInline
value class ScopePathImpl(private val pathSegments: ArrayList<Any>) : ScopePath {

	override fun toString(): String = buildString { pathSegments.forEach { append(it) } }

	override fun enterScope(scope: Any) {
		pathSegments.addLast(scope)
	}

	override fun enterScopes(vararg scope: Any) {
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
 * Execute the block even if the receiver is null, but without appending anything to the path in that case.
 */
inline fun <T> ScopePath?.appending(toAppend: Any, block: () -> T): T {
	this?.enterScope(toAppend)
	return try {
		block()
	} finally {
		this?.exitScope()
	}
}

// 2 and 3 args versions are very common, should be a bit faster than using vararg at least for ScopePathImpl
inline fun <T> ScopePath?.appending(toAppend1: Any, toAppend2: Any, block: () -> T): T {
	this?.apply {
		enterScope(toAppend1)
		enterScope(toAppend2)
	}
	return try {
		block()
	} finally {
		this?.exitScopes(2)
	}
}

// 2 and 3 args versions are very common, should be a bit faster than using vararg at least for ScopePathImpl
inline fun <T> ScopePath?.appending(toAppend1: Any, toAppend2: Any, toAppend3: Any, block: () -> T): T {
	this?.apply {
		enterScope(toAppend1)
		enterScope(toAppend2)
		enterScope(toAppend3)
	}
	return try {
		block()
	} finally {
		this?.exitScopes(3)
	}
}

/**
 * Appends the given strings to the scope path for the duration of the [block], then removes it.
 * Execute the block even if the receiver is null, but without appending anything to the path in that case.
 */
inline fun <T> ScopePath?.appending(vararg toAppend: Any, block: () -> T): T {
	this?.enterScopes(*toAppend)
	return try {
		block()
	} finally {
		this?.exitScopes(toAppend.size)
	}
}