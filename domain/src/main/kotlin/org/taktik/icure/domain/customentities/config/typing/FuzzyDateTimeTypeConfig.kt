package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.LongNode
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.utils.FuzzyDates
import java.time.temporal.ChronoUnit

/**
 * Type for fuzzy datetime, with or without precision encoding.
 */
data class FuzzyDateTimeTypeConfig(
	val nullable: Boolean = false,
	val allowPrecisionEncoding: Boolean = false
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: JsonNode
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is LongNode) {
			"$path: invalid type, expected Long (fuzzy date time)"
		}
		val parsed = FuzzyDates.getLocalDateTimeWithPrecision(value.asLong(), false)
		requireNotNull(parsed) {
			"$path: invalid fuzzy date time"
		}
		if (!allowPrecisionEncoding) {
			require(parsed.second == ChronoUnit.SECONDS) {
				"$path: precision encoding is not allowed"
			}
		}
		value
	}
}
