package org.taktik.icure.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.utils.FuzzyDates
import java.time.temporal.ChronoUnit

/**
 * Type for fuzzy date, with or without precision encoding.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class FuzzyDateTypeConfig(
	override val nullable: Boolean = false,
	val allowPrecisionEncoding: Boolean = false
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is FuzzyDateTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	override fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (value !is RawJson.JsonInteger) {
			context.validation.addError("GE-FUZZYDATE-JSON")
		} else {
			val parsed = value.asExactIntOrNull()?.let { FuzzyDates.getLocalDateWithPrecision(it) }
			if (parsed == null) {
				context.validation.addError(
					"GE-FUZZYDATE-PARSE",
					"value" to value.value,
				)
			} else if (!allowPrecisionEncoding) {
				if (parsed.second != ChronoUnit.DAYS) {
					context.validation.addError(
						"GE-FUZZYDATE-PRECISION",
						"value" to value.value,
						"precision" to parsed.second.name
					)
				}
			}
		}
		value
	}
}
