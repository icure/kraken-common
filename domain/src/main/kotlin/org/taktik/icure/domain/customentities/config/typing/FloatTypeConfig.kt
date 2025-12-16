package org.taktik.icure.domain.customentities.config.typing

import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

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
data class FloatTypeConfig(
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null
) : GenericTypeConfig {
	data class ValidationConfig(
		val min: Double? = null,
		val max: Double? = null,
		val exclusiveMin: Boolean = false,
		val exclusiveMax: Boolean = false
	)

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
	) {
		validation?.apply {
			require((max ?: Double.MAX_VALUE) > (min ?: Double.MIN_VALUE)) {
				"$path: invalid float validation config, value for `max` should be greater than value for `min`"
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonNumber) {
			"$path: invalid type, expected Float64"
		}
		val valueDouble = requireNotNull(value.asDouble().takeIf { it.isFinite() }) {
			"$path: value can't be represented using a finite 64-bit floating point number"
		}
		if (validation != null) {
			require (
				validation.min?.let { min -> if (validation.exclusiveMin) valueDouble > min else valueDouble >= min } ?: true
					&& validation.max?.let { max -> if (validation.exclusiveMax) valueDouble < max else valueDouble <= max } ?: true
			) {
				"$path: value $valueDouble out of configured bounds"
			}
		}
		RawJson.JsonDecimal(valueDouble.toString())
	}
}
