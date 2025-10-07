package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.utils.FuzzyDates
import java.time.temporal.ChronoUnit

/**
 * Type for fuzzy time, with or without precision encoding.
 */
data class FuzzyTimeTypeConfig(
	val nullable: Boolean = false,
	val allowPrecisionEncoding: Boolean = false
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: JsonNode
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is IntNode) {
			"$path: invalid type, expected Int (fuzzy time)"
		}
		val parsed = FuzzyDates.getLocalTimeWithPrecision(value.asInt())
		requireNotNull(parsed) {
			"$path: invalid fuzzy time"
		}
		if (!allowPrecisionEncoding) {
			require(parsed.second == ChronoUnit.SECONDS) {
				"$path: precision encoding is not allowed"
			}
		}
		value
	}
}
