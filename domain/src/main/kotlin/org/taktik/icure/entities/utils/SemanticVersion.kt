package org.taktik.icure.entities.utils

@JvmInline
value class SemanticVersion(val version: String) : Comparable<SemanticVersion> {

	private fun asIntComponentNoSuffix(component: String): Int = buildString {
		component
			.takeWhile { it in '0'..'9' }
			.forEach { append(it) }
	}.toInt()

	private fun getComponentSuffix(component: String): String = component.dropWhile {
		it in '0'..'9'
	}

	override fun compareTo(other: SemanticVersion): Int {
		val thisSplit = version.split('.').also {
			require(it.size == 3) { "Invalid version format: $version" }
		}
		val otherSplit = other.version.split('.').also {
			require(it.size == 3) { "Invalid version format: ${other.version}" }
		}
		return thisSplit[0].toInt().compareTo(otherSplit[0].toInt()).takeIf { it != 0 }
			?: thisSplit[1].toInt().compareTo(otherSplit[1].toInt()).takeIf { it != 0 }
			?: asIntComponentNoSuffix(thisSplit[2]).compareTo(asIntComponentNoSuffix(otherSplit[2])).takeIf { it != 0 }
			?: getComponentSuffix(thisSplit[2]).let { thisSuffix ->
				val otherSuffix = getComponentSuffix(otherSplit[2])
				when {
					thisSuffix.isEmpty() && otherSuffix.isEmpty() -> 0
					thisSuffix.isEmpty() -> 1
					otherSuffix.isEmpty() -> -1
					else -> thisSuffix.compareTo(otherSuffix)
				}
			}
	}
}