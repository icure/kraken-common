package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.entities.RawJson

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
			path: ResolutionPath
		) {
			require(minLength == null || minLength > 0) {
				"$path: invalid minLength, should be greater than 0"
			}
			require(maxLength == null || maxLength > 0) {
				"$path: invalid maxLength, should be greater than 0"
			}
			require(minLength == null || maxLength == null || maxLength >= minLength) {
				"$path: invalid length bounds, maxLength should be greater than or equal to minLength"
			}
		}

		fun validateValue(
			path: ResolutionPath,
			value: String
		) {
			require(
				(minLength == null || value.length >= minLength)
					&& (maxLength == null || value.length <= maxLength)
			) {
				"$path: string length out of bounds"
			}
		}
	}

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath
	) {
		validation?.validateConfig(path)
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonString) {
			"$path: invalid type, expected Text (string)"
		}
		validation?.validateValue(path, value.value)
		value
	}
}