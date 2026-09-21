package org.taktik.icure.entities.utils

class SemanticVersion(val version: String) : Comparable<SemanticVersion> {
	companion object {
		private fun parse(value: String): Parsed {
			require (value.length < 100) { "Invalid version format: $value" }
			val thisSplit = value.split('.', limit = 3).also {
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

		/**
		 * Splits a suffix in its identifiers, treating both `.` (standard semver, `3.0.0-PREVIEW.11`) and `-`
		 * (legacy style, `3.0.0-PREVIEW-11`) as separators, so that the two conventions compare consistently.
		 */
		private fun suffixIdentifiers(suffix: String): List<String> =
			suffix.split('.', '-').filter { it.isNotEmpty() }

		/**
		 * Compares two suffix identifiers following the semver rules: purely numeric identifiers are compared
		 * numerically and have always lower precedence than alphanumeric ones, which are compared lexicographically.
		 */
		private fun compareIdentifiers(a: String, b: String): Int {
			val aInt = a.toIntOrNull()
			val bInt = b.toIntOrNull()
			return when {
				aInt != null && bInt != null -> aInt.compareTo(bInt)
				aInt != null -> -1
				bInt != null -> 1
				else -> a.compareTo(b)
			}
		}

		private fun compareSuffixes(a: String, b: String): Int {
			if (a == b) return 0
			val aIds = suffixIdentifiers(a)
			val bIds = suffixIdentifiers(b)
			for (i in 0 until minOf(aIds.size, bIds.size)) {
				compareIdentifiers(aIds[i], bIds[i]).takeIf { it != 0 }?.let { return it }
			}
			return aIds.size.compareTo(bIds.size).takeIf { it != 0 }
				// Suffixes written in different conventions but with the same identifiers (`-PREVIEW-11` and
				// `-PREVIEW.11`): fall back to a lexicographic comparison to keep the ordering total.
				?: a.compareTo(b)
		}
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
					else -> compareSuffixes(suffix, other.suffix)
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

		return true
	}

	override fun hashCode(): Int {
		return version.hashCode()
	}
}
