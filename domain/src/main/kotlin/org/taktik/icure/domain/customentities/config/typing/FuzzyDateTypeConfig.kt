package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector
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
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonInteger) {
			validationContext.addError("Invalid type, expected Int (fuzzy date)")
		} else {
			val parsed = value.asExactIntOrNull()?.let { FuzzyDates.getLocalDateWithPrecision(it) }
			if (parsed == null) {
				validationContext.addError("Invalid fuzzy date")
			} else if (!allowPrecisionEncoding) {
				if (parsed.second != ChronoUnit.DAYS) {
					validationContext.addError("Precision encoding is not allowed")
				}
			}
		}
		value
	}
}
