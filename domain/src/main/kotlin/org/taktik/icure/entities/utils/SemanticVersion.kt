package org.taktik.icure.entities.utils

/**
 * A `major.minor.patch[suffix]` version, ordered following the semantic versioning precedence rules:
 * - major, minor and patch are compared numerically;
 * - a version without suffix (a release) is greater than the same version with a suffix (a pre-release), e.g.
 *   `3.0.0-preview-6 < 3.0.0`;
 * - pre-release suffixes are compared identifier by identifier. Identifiers are separated by `.` or `-`, and
 *   runs of digits and runs of letters are distinct identifiers (`rc10` is `rc`, `10`). Numeric identifiers are
 *   compared numerically and are lower than alphanumeric ones, which are compared lexically. If all identifiers
 *   are equal, the suffix with fewer identifiers is lower. For example `3.0.0-preview-6 < 3.0.0-preview-10` and
 *   `1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-beta < 1.0.0-rc2 < 1.0.0-rc10`.
 * - build metadata (anything after a `+`) is ignored for ordering.
 */
class SemanticVersion(val version: String) : Comparable<SemanticVersion> {
	companion object {
		private fun parse(value: String): Parsed {
			val thisSplit = value.substringBefore('+').split('.', limit = 3).also {
				require(it.size == 3) { "Invalid version format: $value" }
			}
			return Parsed(
				thisSplit[0].toInt(),
				thisSplit[1].toInt(),
				asIntComponentNoSuffix(thisSplit[2]),
				preReleaseIdentifiers(getComponentSuffix(thisSplit[2])).takeIf { it.isNotEmpty() }
			)
		}

		private fun asIntComponentNoSuffix(component: String): Int =
			component.takeWhile { it in '0'..'9' }.toInt()

		private fun getComponentSuffix(component: String): String =
			component.dropWhile { it in '0'..'9' }

		private val IDENTIFIER_REGEX = Regex("[0-9]+|[^0-9.\\-]+")

		private fun preReleaseIdentifiers(suffix: String): List<String> =
			IDENTIFIER_REGEX.findAll(suffix).map { it.value }.toList()

		private fun String.isNumeric() = all { it in '0'..'9' }

		private fun compareIdentifiers(a: String, b: String): Int {
			val aNumeric = a.isNumeric()
			val bNumeric = b.isNumeric()
			return when {
				aNumeric && bNumeric -> {
					// Compare without converting to a number to support arbitrarily long numeric identifiers
					val aTrimmed = a.trimStart('0')
					val bTrimmed = b.trimStart('0')
					aTrimmed.length.compareTo(bTrimmed.length).takeIf { it != 0 } ?: aTrimmed.compareTo(bTrimmed)
				}
				aNumeric -> -1
				bNumeric -> 1
				else -> a.compareTo(b)
			}
		}

		private fun comparePreRelease(a: List<String>, b: List<String>): Int {
			a.zip(b).forEach { (aId, bId) ->
				compareIdentifiers(aId, bId).takeIf { it != 0 }?.let { return it }
			}
			return a.size.compareTo(b.size)
		}
	}

	private class Parsed(val major: Int, val minor: Int, val patch: Int, val preRelease: List<String>?): Comparable<Parsed> {
		override fun compareTo(other: Parsed): Int =
			major.compareTo(other.major).takeIf { it != 0 }
				?: minor.compareTo(other.minor).takeIf { it != 0 }
				?: patch.compareTo(other.patch).takeIf { it != 0 }
				?: when {
					preRelease == null && other.preRelease == null -> 0
					preRelease == null -> 1
					other.preRelease == null -> -1
					else -> comparePreRelease(preRelease, other.preRelease)
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
		return version == other.version
	}

	override fun hashCode(): Int = version.hashCode()

	override fun toString(): String = version
}
