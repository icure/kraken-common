package org.taktik.icure.customentities.config.typing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.JsonIncludeValue
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.customentities.util.CustomEntityValueValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.addWarning

@JsonInclude(JsonIncludeValue.NON_DEFAULT)
@SerialName("Int")
@Serializable
data class IntTypeConfig(
	override val nullable: Boolean = false,
	val validation: ValidationConfig? = null
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is IntTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	companion object {
		/**
		 * Biggest long that we can safely store without incurring in any issues with js numbers or couchdb views
		 */
		const val MAX_SAFE_LONG = 9007199254740991
		/**
		 * Smallest long that we can safely store without incurring in any issues with js numbers or couchdb views
		 */
		const val MIN_SAFE_LONG = -9007199254740991
	}

	@JsonInclude(JsonIncludeValue.NON_DEFAULT)
	@Serializable
	data class ValidationConfig(
		/**
		 * Minimum acceptable value, always inclusive.
		 * If null then it is considered -9007199254740991 (matching javascript's Number.MIN_SAFE_INTEGER)
		 */
		val min: Long? = null,
		/**
		 * Maximum acceptable value, always inclusive.
		 * If null then it is considered 9007199254740991 (matching javascript's Number.MAX_SAFE_INTEGER)
		 */
		val max: Long? = null,
	)

	override fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {
		validation?.apply {
			if (min != null && min !in MIN_SAFE_LONG..MAX_SAFE_LONG) {
				context.validation.addError("GED-INT-MIN")
			}
			if (max != null && max !in MIN_SAFE_LONG..MAX_SAFE_LONG) {
				context.validation.addError("GED-INT-MAX")
			}
			val minOrDefault = min ?: MIN_SAFE_LONG
			val maxOrDefault = max ?: MAX_SAFE_LONG
			if (maxOrDefault < minOrDefault) {
				context.validation.addError("GED-INT-NORANGE")
			} else if (minOrDefault == maxOrDefault) {
				context.validation.addWarning(
					"GED-INT-WONE",
					"value" to minOrDefault
				)
			}
			if (min == MIN_SAFE_LONG) { // do not use minOrDefault here
				context.validation.addWarning("GED-INT-WMIN")
			}
			if (max == MAX_SAFE_LONG) { // do not use maxOrDefault here
				context.validation.addWarning("GED-INT-WMAX")
			}
		}
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityValueValidationContext,
		value: RawJson
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (value !is RawJson.JsonInteger) {
			context.validation.addError("GEV-INT-JSON")
		} else {
			if (
				value.value < (validation?.min ?: MIN_SAFE_LONG) || value.value > (validation?.max ?: MAX_SAFE_LONG)
			) {
				context.validation.addError(
					"GEV-INT-OUTRANGE",
					"value" to value.value,
					"min" to (validation?.min ?: MIN_SAFE_LONG),
					"max" to (validation?.max ?: MAX_SAFE_LONG),
				)
			}
		}
		value
	}
}