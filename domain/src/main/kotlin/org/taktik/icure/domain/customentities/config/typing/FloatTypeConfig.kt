package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.addWarning

/**
 * Type for 64-bit floating point fields.
 *
 * The json value will be converted to a 64-bit floating point number before storage: this may lead to loss of precision
 * for some numbers, and may change the representation of the content.
 * Additionally, numbers that are too large and would be represented as Infinity, will be rejected.
 * ```
 * 0.12345678912345678 -> 0.12345678912345678 (same)
 * 12345678912345678 -> 1.2345678912345678E16 (representation change, but equivalent value)
 * 1.00 -> 1.0 (representation change, equivalent value but not acceptable under some conditions, e.g. FHIR)
 * 0.123456789123456789 -> 0.12345678912345678 (precision loss)
 * 1e400 -> REJECTED (too large, would be infinity)
 * ```
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class FloatTypeConfig(
	override val nullable: Boolean = false,
	val validation: ValidationConfig? = null
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is FloatTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	data class ValidationConfig(
		val min: Double? = null,
		val max: Double? = null,
		val exclusiveMin: Boolean = false,
		val exclusiveMax: Boolean = false
	)

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
	) {
		validation?.apply {
			if (validation.min != null && !validation.min.isFinite()) {
				validationContext.addError("GE-FLOAT-MIN")
			}
			if (validation.max != null && !validation.max.isFinite()) {
				validationContext.addError("GE-FLOAT-MAX")
			}
			val minOrDefault = validation.min ?: -Double.MAX_VALUE
			val maxOrDefault = validation.max ?: Double.MAX_VALUE
			if (minOrDefault > maxOrDefault) {
				validationContext.addError("GE-FLOAT-NORANGE")
			} else if (minOrDefault == maxOrDefault) {
				if (exclusiveMin || exclusiveMax) {
					validationContext.addError("GE-FLOAT-NORANGE")
				} else {
					validationContext.addWarning(
						"GE-FLOAT-WONE",
						"value" to minOrDefault
					)
				}
			}
			if (validation.min == -Double.MAX_VALUE && !validation.exclusiveMin) { // do not use minOrDefault here
				validationContext.addWarning("GE-FLOAT-WMIN")
			}
			if (validation.max == Double.MAX_VALUE && !validation.exclusiveMax) { // do not use maxOrDefault here
				validationContext.addWarning("GE-FLOAT-WMAX")
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonNumber) {
			validationContext.addError("GE-FLOAT-JSON")
			value
		} else {
			val valueDouble = value.asDouble()
			if (!valueDouble.isFinite()) {
				validationContext.addError("GE-FLOAT-INFINITE")
			}
			if (validation != null) {
				if (
					validation.min?.let { min -> if (validation.exclusiveMin) valueDouble <= min else valueDouble < min } ?: false
					|| validation.max?.let { max -> if (validation.exclusiveMax) valueDouble >= max else valueDouble > max } ?: false
				) {
					validationContext.addError(
						"GE-FLOAT-OUTRANGE",
						"value" to valueDouble,
					)
				}
			}
			RawJson.JsonFloat(valueDouble)
		}
	}
}
