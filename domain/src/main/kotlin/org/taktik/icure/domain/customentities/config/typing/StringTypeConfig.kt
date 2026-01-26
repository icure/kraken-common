package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector

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
			if (minLength != null && minLength <= 0) {
				context.addError("Invalid minLength, should be greater than 0")
			}
			if (maxLength != null && maxLength <= 0) {
				context.addError("Invalid maxLength, should be greater than 0")
			}
			if (minLength != null && maxLength != null && maxLength < minLength) {
				context.addError("Invalid length bounds, maxLength should be greater than or equal to minLength")
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
				context.addError("String length out of bounds")
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
			validationContext.addError("Invalid type, expected Text (string)")
		} else {
			validation?.validateValue(validationContext, value.value)
		}
		value
	}
}