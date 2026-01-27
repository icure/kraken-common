package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector

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
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null
) : GenericTypeConfig {
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
				validationContext.addError("Invalid float validation config, value for `min` must be a finite float")
			}
			if (validation.max != null && !validation.max.isFinite()) {
				validationContext.addError("Invalid float validation config, value for `max` must be a finite float")
			}
			if (validation.min != null && validation.max != null) {
				if (validation.min > validation.max || (validation.min == validation.max && (validation.exclusiveMin || validation.exclusiveMax))) {
					validationContext.addError("Invalid float validation config, range has no valid values")
				}
			}
			if (validation.min == -Double.MAX_VALUE && !validation.exclusiveMin) {
				validationContext.addWarning("Redundant float validation config, inclusive `min` matches the minimum allowed value")
			}
			if (validation.max == Double.MAX_VALUE && !validation.exclusiveMax) {
				validationContext.addWarning("Redundant float validation config, inclusive `max` matches the maximum allowed value")
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonNumber) {
			validationContext.addError("Invalid type, expected Float64")
			value
		} else {
			val valueDouble = value.asDouble()
			if (!valueDouble.isFinite()) validationContext.addError(
				"Value can't be represented using a finite 64-bit floating point number"
			)
			if (validation != null) {
				if (
					validation.min?.let { min -> if (validation.exclusiveMin) valueDouble <= min else valueDouble < min } ?: false
					|| validation.max?.let { max -> if (validation.exclusiveMax) valueDouble >= max else valueDouble > max } ?: false
				) validationContext.addError("Value $valueDouble out of configured bounds")
			}
			RawJson.JsonFloat(valueDouble)
		}
	}
}
