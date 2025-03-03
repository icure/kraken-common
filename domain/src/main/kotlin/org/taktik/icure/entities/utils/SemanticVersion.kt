package org.taktik.icure.entities.utils

private val semVerRegex = Regex("^([0-9]+)\\.([0-9]+)\\.([0-9]+)([\\-A-Za-z0-9.]*)\$")

@JvmInline
value class SemanticVersion(val version: String) : Comparable<SemanticVersion> {

	init {
		require(version.matches(semVerRegex)) {
			"Invalid semantic version syntax"
		}
	}

	override fun compareTo(other: SemanticVersion): Int {
		val thisMatch = checkNotNull(semVerRegex.find(version)) {
			"Cannot match version: $version"
		}
		val otherMatch =checkNotNull(semVerRegex.find(other.version)) {
			"Cannot match version: $version"
		}
		return thisMatch.groupValues[1].toInt().compareTo(otherMatch.groupValues[1].toInt()).takeIf { it != 0 }
			?: thisMatch.groupValues[2].toInt().compareTo(otherMatch.groupValues[2].toInt()).takeIf { it != 0 }
			?: thisMatch.groupValues[3].toInt().compareTo(otherMatch.groupValues[3].toInt()).takeIf {
				it != 0 || (thisMatch.groupValues.size == 3 && otherMatch.groupValues.size == 4)
			} ?: thisMatch.groupValues.getOrElse(4) { "" }.compareTo(otherMatch.groupValues.getOrElse(4) { "" })
	}

}
