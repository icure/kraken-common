package org.taktik.icure.domain.customentities.util

@JvmInline
value class ResolutionPath(val pathSegments: ArrayList<String>) {
	companion object {
		operator fun invoke(vararg pathSegments: String): ResolutionPath =
			ResolutionPath(arrayListOf(*pathSegments))
	}

	override fun toString(): String = buildString { pathSegments.forEach { append(it) } }

	inline fun <T> appending(toAppend: String, block: () -> T): T {
		pathSegments.addLast(toAppend)
		return try {
			block()
		} finally {
			pathSegments.removeLast()
		}
	}

	inline fun <T> appending(vararg toAppend: String, block: () -> T): T {
		pathSegments.addAll(toAppend.asList())
		return try {
			block()
		} finally {
			repeat(toAppend.size) {
				pathSegments.removeLast()
			}
		}
	}
}