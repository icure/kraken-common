package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.utils.FuzzyDates
import java.time.temporal.ChronoUnit

/**
 * Type for fuzzy datetime, with or without precision encoding.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class FuzzyDateTimeTypeConfig(
	override val nullable: Boolean = false,
	val allowPrecisionEncoding: Boolean = false
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonInteger) {
			validationContext.addError("GE-FUZZYDATETIME-JSON")
		} else {
			val parsed = FuzzyDates.getLocalDateTimeWithPrecision(value.value, false)
			if (parsed == null) {
				validationContext.addError(
					"GE-FUZZYDATETIME-PARSE",
					"value" to value.value,
				)
			} else if (!allowPrecisionEncoding) {
				if (parsed.second != ChronoUnit.SECONDS) validationContext.addError(
					"GE-FUZZYDATETIME-PRECISION",
					"value" to value.value,
					"precision" to parsed.second.name,
				)
			}
		}
		value
	}
}
