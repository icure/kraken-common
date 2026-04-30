package org.taktik.icure.entities.utils

class SemanticVersion(val version: String) : Comparable<SemanticVersion> {
	companion object {
		private fun parse(value: String): Parsed {
			val thisSplit = value.split('.').also {
				require(it.size == 3) { "Invalid version format: $value" }
			}
			return Parsed(
				thisSplit[0].toInt(),
				thisSplit[1].toInt(),
				asIntComponentNoSuffix(thisSplit[2]),
				getComponentSuffix(thisSplit[2]).takeIf { it.isNotEmpty() }
			)
		}


		private fun asIntComponentNoSuffix(component: String): Int =
			component.takeWhile { it in '0'..'9' }.toInt()

		private fun getComponentSuffix(component: String): String =
			component.dropWhile { it in '0'..'9' }
	}

	private class Parsed(val major: Int, val minor: Int, val patch: Int, val suffix: String?): Comparable<Parsed> {
		override fun compareTo(other: Parsed): Int =
			major.compareTo(other.major).takeIf { it != 0 }
				?: minor.compareTo(other.minor).takeIf { it != 0 }
				?: patch.compareTo(other.patch).takeIf { it != 0 }
				?: when {
					suffix == null && other.suffix == null -> 0
					suffix == null -> 1
					other.suffix == null -> -1
					else -> suffix.compareTo(other.suffix)
				}
	}

	private val parsed by lazy(LazyThreadSafetyMode.PUBLICATION) {
		parse(version)
	}

	override fun compareTo(other: SemanticVersion): Int {
		return this.parsed.compareTo(other.parsed)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is SemanticVersion) return false

		if (version != other.version) return false
		if (parsed != other.parsed) return false

		return true
	}

	override fun hashCode(): Int {
		var result = version.hashCode()
		result = 31 * result + parsed.hashCode()
		return result
	}
}