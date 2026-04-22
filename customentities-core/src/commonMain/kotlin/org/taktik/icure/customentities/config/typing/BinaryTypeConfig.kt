package org.taktik.icure.customentities.config.typing

import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.customentities.util.CustomEntityValueValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.JsonIncludeValue

/**
 * Allow to store some binary values directly in the document json as Base64 (standard alphabet with + and /, not
 * url-safe alphabet).
 *
 * This should be used only for very small binary data (at most a few kilobytes), large binary data should be stored
 * using the attachments system.
 */
@JsonInclude(JsonIncludeValue.NON_DEFAULT)
data class BinaryTypeConfig(
	val validation: ValidationConfig? = null,
	override val nullable: Boolean = false
) : GenericTypeConfig {

	data class ValidationConfig(
		/**
		 * Max size in bytes
		 */
		val maxSize: Int?,
	)

	override fun validateConfig(context: CustomEntityConfigValidationContext) {
		validation?.apply {
			maxSize?.also {
				if (it <= 0) context.validation.addError("GE-BINARY-MAXSIZE-NONPOSITIVE")
			}
		}
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityValueValidationContext,
		value: RawJson
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (value !is RawJson.JsonString) {
			context.validation.addError("GE-BINARY-JSON")
		} else {
			validation?.maxSize?.also {
				if (value.value.length > (it + 2) / 3 * 4) {
					context.validation.addError("GE-BINARY-TOOBIG", "maxSize" to it)
					return@validatingNullForStore value
				}
			}
			val decodedSize = validBase64DecodedSize(value.value)
			if (decodedSize == null) {
				context.validation.addError("GE-BINARY-INVALID")
			} else {
				validation?.maxSize?.also {
					if (decodedSize > it) {
						context.validation.addError("GE-BINARY-TOOBIG", "maxSize" to it)
					}
				}
			}
		}
		value
	}

	/**
	 * Returns the decoded byte size of a valid standard-alphabet base64 string, or null if the string is not valid.
	 * Padding is optional, but if present it must be of the correct length.
	 */
	private fun validBase64DecodedSize(str: String): Int? {
		val len = str.length
		val paddingCount = when {
			len >= 2 && str[len - 2] == '=' -> if (str[len - 1] == '=') 2 else return null
			len >= 1 && str[len - 1] == '=' -> 1
			else -> 0
		}
		val dataLen = len - paddingCount
		val remainder = dataLen % 4
		if (remainder == 1) return null
		if (paddingCount != 0 && paddingCount != (4 - remainder) % 4) return null
		for (i in 0 until dataLen) {
			val c = str[i]
			if (c !in 'A'..'Z' && c !in 'a'..'z' && c !in '0'..'9' && c != '+' && c != '/') return null
		}
		return dataLen * 3 / 4
	}

	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is BinaryTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))
}
