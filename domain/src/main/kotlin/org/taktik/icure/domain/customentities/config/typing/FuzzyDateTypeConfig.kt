package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.utils.FuzzyDates
import java.time.temporal.ChronoUnit

/**
 * Type for fuzzy date, with or without precision encoding.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class FuzzyDateTypeConfig(
	val nullable: Boolean = false,
	val allowPrecisionEncoding: Boolean = false
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonInteger) {
			"$path: invalid type, expected Int (fuzzy date)"
		}
		val parsed = value.asExactIntOrNull()?.let { FuzzyDates.getLocalDateWithPrecision(it) }
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
