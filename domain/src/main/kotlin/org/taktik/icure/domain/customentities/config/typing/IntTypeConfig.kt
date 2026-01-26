package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class IntTypeConfig(
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null
) : GenericTypeConfig {
	companion object {
		/**
		 * Biggest long that we can safely store without incurring in any issues with js numbers or couchdb views
		 */
		private const val MAX_SAFE_LONG = 9007199254740991
		/**
		 * Smallest long that we can safely store without incurring in any issues with js numbers or couchdb views
		 */
		private const val MIN_SAFE_LONG = -9007199254740991
	}

	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	data class ValidationConfig(
		/**
		 * Minimum acceptable value, always inclusive.
		 * If null, then it is considered as the MIN safe integer value representable by a 64-bit floating point number
		 */
		val min: Long? = null,
		/**
		 * Maximum acceptable value, always inclusive.
		 * If null, then it is considered as the MAX safe integer value representable by a 64-bit floating point number
		 */
		val max: Long? = null,
	)

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
	) {
		validation?.apply {
			if (min != null && min !in MIN_SAFE_LONG..MAX_SAFE_LONG) {
				validationContext.addError("Invalid int validation config, value for `min` is not within the safe range for JS integers")
			}
			if (max != null && max !in MIN_SAFE_LONG..MAX_SAFE_LONG) {
				validationContext.addError("Invalid int validation config, value for `max` is not within the safe range for JS integers")
			}
			if ((max ?: MAX_SAFE_LONG) <= (min ?: MIN_SAFE_LONG)) {
				validationContext.addError("Invalid int validation config, value for `max` should be greater than value for `min`")
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonInteger) {
			validationContext.addError("Invalid type, expected Int64")
		} else {
			if (
				value.value < (validation?.min ?: MIN_SAFE_LONG) || value.value > (validation?.max ?: MAX_SAFE_LONG)
			) {
				validationContext.addError("Value ${value.value} out of configured bounds")
			}
		}
		value
	}
}