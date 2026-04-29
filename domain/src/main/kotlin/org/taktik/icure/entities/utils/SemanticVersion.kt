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
		return thisSplit[1].toInt().compareTo(otherSplit[1].toInt()).takeIf { it != 0 }
			?: thisSplit[2].toInt().compareTo(otherSplit[2].toInt()).takeIf { it != 0 }
			?: asIntComponentNoSuffix(thisSplit[3]).compareTo(asIntComponentNoSuffix(otherSplit[3])).takeIf { it != 0 }
			?: getComponentSuffix(thisSplit[3]).compareTo(getComponentSuffix(otherSplit[3]))
	}
}
