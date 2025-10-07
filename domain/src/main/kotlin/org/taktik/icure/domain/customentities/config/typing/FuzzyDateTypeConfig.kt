package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.utils.FuzzyDates
import java.time.temporal.ChronoUnit

/**
 * Type for fuzzy date, with or without precision encoding.
 */
data class FuzzyDateTypeConfig(
	val nullable: Boolean = false,
	val allowPrecisionEncoding: Boolean = false
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: JsonNode
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is IntNode) {
			"$path: invalid type, expected Int (fuzzy date)"
		}
		val parsed = FuzzyDates.getLocalDateWithPrecision(value.asInt())
		requireNotNull(parsed) {
			"$path: invalid fuzzy date"
		}
		if (!allowPrecisionEncoding) {
			require(parsed.second == ChronoUnit.DAYS) {
				"$path: precision encoding is not allowed"
			}
		}
		value
	}
}
