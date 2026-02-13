package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError

/**
 * Type for a UUID string (not necessarily v4 UUID).
 * You can specify the expected format using [format], default (if left null) is [Format.LOWER_DASHED].
 */
//TODO
// uuid should be independent of representation. Should maybe not allow to specify format, and enforce one of our chosing (maybe even Base64),
// Could be annoying for debug and could prohibit certain queries by range if we use a base64 though.
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class UuidTypeConfig(
	override val nullable: Boolean = false,
	val format: Format? = null,
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is UuidTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	companion object {
		@JvmStatic
		private val DIGITS = '0'..'9'
		@JvmStatic
		private val LOWERCASE_A_TO_F = 'a'..'f'
		@JvmStatic
		private val UPPERCASE_A_TO_F = 'A'..'F'
		@JvmStatic
		private fun checkDashed(uuidString: String, acceptableLetters: CharRange): Boolean {
			if (uuidString.length != 36) return false
			uuidString.forEachIndexed { i, c ->
				if (i == 8 || i == 13 || i == 18 || i == 23) {
					if (c != '-') return false
				} else {
					if (c !in DIGITS && c !in acceptableLetters) return false
				}
			}
			return true
		}
	}

	// Custom lambda is less pretty than regex, but is 3x faster on good input and over 10x faster on bad input
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	enum class Format(val validate: (String) -> Boolean) {
		/**
		 * Dash-separated hexadecimal representation with lowercase letters, groups of 8-4-4-4-12 digits.
		 * Example: "7cc5ed00-dbf2-11f0-b558-0800200c9a66"
		 */
		LOWER_DASHED({ checkDashed(it, LOWERCASE_A_TO_F) }),
		/**
		 * Dash-separated hexadecimal representation with uppercase letters, groups of 8-4-4-4-12 digits.
		 * Example: "7CC5ED00-DBF2-11F0-B558-0800200C9A66"
		 */
		UPPER_DASHED({ checkDashed(it, UPPERCASE_A_TO_F) }),
		/**
		 * Compact hexadecimal representation with lowercase letters and no dashes to separate in groups.
		 * Example: "7cc5ed00dbf211f0b5580800200c9a66"
		 */
		LOWER_COMPACT({ s -> s.length == 32 && s.all { it in LOWERCASE_A_TO_F || it in DIGITS }}),
		/**
		 * Compact hexadecimal representation with uppercase letters and no dashes to separate in groups.
		 * Example: "7CC5ED00DBF211F0B5580800200C9A66"
		 */
		UPPER_COMPACT({ s -> s.length == 32 && s.all { it in UPPERCASE_A_TO_F || it in DIGITS }})
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson = validatingNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonString) {
			validationContext.addError("GE-UUID-JSON")
		} else {
			val formatOrDefault = format ?: Format.LOWER_DASHED
			if (!formatOrDefault.validate(value.value)) {
				validationContext.addError(
					"GE-UUID-FORMAT",
					"format" to formatOrDefault.name,
					"value" to truncateValueForErrorMessage(value.value)
				)
			}
		}
		value
	}
}
