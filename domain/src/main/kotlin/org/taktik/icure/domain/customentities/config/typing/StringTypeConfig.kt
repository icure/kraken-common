package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.addWarning

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class StringTypeConfig(
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null
) : GenericTypeConfig {
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	data class ValidationConfig(
		/**
		 * Minimum length of the string (inclusive).
		 * The length is the number of 16-bit Unicode characters needed to represent the string.
		 * When using some special characters (like emojis), this might not correspond to the number of user-perceived
		 * characters.
		 * Most common characters (like latin letters) are represented as a single 16-bit Unicode character.
		 */
		val minLength: Int? = null,
		/**
		 * Maximum length of the string (inclusive).
		 * Refer to [minLength] documentation for details about how length is computed.
		 */
		val maxLength: Int? = null,
	) {
		fun validateConfig(
			context: ScopedErrorCollector
		) {
			if (minLength != null && minLength < 0) {
				context.addError("GE-STRING-MIN")
			}
			if (maxLength != null && maxLength < 0) {
				context.addError("GE-STRING-MAX")
			}
			if (minLength != null && maxLength != null && maxLength < minLength) {
				context.addError("GE-STRING-NORANGE")
			}
			if (maxLength == 0) {
				context.addWarning("GE-STRING-WEMPTY")
			}
			if (minLength == 0) {
				context.addWarning("GE-STRING-WMIN")
			}
		}

		fun validateValue(
			context: ScopedErrorCollector,
			value: String
		) {
			if (
				(minLength != null && value.length < minLength)
				|| (maxLength != null && value.length > maxLength)
			) {
				context.addError(
					"GE-STRING-OUTRANGE",
					"length" to value.length,
					"min" to (minLength ?: "0"),
					"max" to (maxLength ?: "*"),
				)
			}
		}
	}

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector
	) {
		validation?.validateConfig(validationContext)
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonString) {
			validationContext.addError("GE-STRING-JSON")
		} else {
			validation?.validateValue(validationContext, value.value)
		}
		value
	}
}