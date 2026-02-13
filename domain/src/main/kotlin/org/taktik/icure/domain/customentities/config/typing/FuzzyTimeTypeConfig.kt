package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.utils.FuzzyDates
import java.time.temporal.ChronoUnit

/**
 * Type for fuzzy time, with or without precision encoding.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class FuzzyTimeTypeConfig(
	override val nullable: Boolean = false,
	val allowPrecisionEncoding: Boolean = false
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is FuzzyTimeTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonInteger) {
			validationContext.addError("GE-FUZZYTIME-JSON")
		} else {
			val parsed = value.asExactIntOrNull()?.let { FuzzyDates.getLocalTimeWithPrecision(it) }
			if (parsed == null) {
				validationContext.addError("GE-FUZZYTIME-PARSE", "value" to value.value)
			} else if (!allowPrecisionEncoding) {
				if (parsed.second != ChronoUnit.SECONDS) {
					validationContext.addError(
						"GE-FUZZYTIME-PRECISION",
						"value" to value.value,
						"precision" to parsed.second.name
					)
				}
			}
		}
		value
	}
}
