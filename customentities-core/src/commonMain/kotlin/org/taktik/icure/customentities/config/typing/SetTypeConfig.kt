package org.taktik.icure.customentities.config.typing

import org.taktik.icure.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.customentities.util.CustomEntityValueValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.appending
import org.taktik.icure.jackson.annotations.JsonIgnore
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.JsonIncludeValue

/**
 * An unordered collection of unique elements.
 * The order of elements is not significant, and not guaranteed to be preserved between reads and writes.
 *
 * Only [StringTypeConfig], [IntTypeConfig], and [EnumTypeConfig] are allowed as element types.
 *
 * Duplicate elements are rejected at store time.
 */
@JsonInclude(JsonIncludeValue.NON_DEFAULT)
data class SetTypeConfig(
	val elementType: GenericTypeConfig,
	override val nullable: Boolean = false,
	val validation: ValidationConfig? = null,
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is SetTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	override fun areEquivalent(a: RawJson, b: RawJson, resolutionContext: CustomEntityConfigResolutionContext?): Boolean {
		if (a == b) return true
		if (a !is RawJson.JsonArray || b !is RawJson.JsonArray) return false
		return a.items.toSet() == b.items.toSet()
	}

	@get:JsonIgnore
	override val objectDefinitionDependencies: Set<Pair<String, Boolean>> get() =
		elementType.objectDefinitionDependencies

	@get:JsonIgnore
	override val enumDefinitionDependencies: Set<Pair<String, Boolean>> get() =
		elementType.enumDefinitionDependencies

	@JsonInclude(JsonIncludeValue.NON_DEFAULT)
	data class ValidationConfig(
		val minLength: Int? = null,
		val maxLength: Int? = null,
	)

	override fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {
		context.validation.appending("[*]") {
			elementType.validateConfig(context)
		}
		if (
			elementType !is StringTypeConfig
				&& elementType !is IntTypeConfig
				&& elementType !is EnumTypeConfig
		) {
			context.validation.addError("GE-SET-ELEMENTTYPE")
		}
		validation?.apply {
			if (minLength != null && minLength < 0) {
				context.validation.addError("GE-SET-MIN")
			}
			if (maxLength != null && maxLength < 0) {
				context.validation.addError("GE-SET-MAX")
			}
			if (minLength != null && maxLength != null && maxLength < minLength) {
				context.validation.addError("GE-SET-NORANGE")
			} else if (maxLength == 0) {
				context.validation.addWarning("GE-SET-WEMPTY")
			}
			if (minLength == 0) {
				context.validation.addWarning("GE-SET-WMIN")
			}
		}
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityValueValidationContext,
		value: RawJson,
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (value !is RawJson.JsonArray) {
			context.validation.addError("GE-SET-JSON")
			value
		} else {
			val res =
				context.validation.appending("[") {
					value.items.mapIndexed { index, element ->
						context.validation.appending(index, "]") {
							elementType.validateAndMapValueForStore(context, element)
						}
					}
				}
			if (res.toSet().size != res.size) {
				context.validation.addError("GE-SET-DUPLICATES")
			}
			if (validation != null) {
				if (
					(validation.minLength != null && res.size < validation.minLength)
					|| (validation.maxLength != null && res.size > validation.maxLength)
				) {
					context.validation.addError(
						"GE-SET-OUTRANGE",
						"length" to res.size,
						"min" to (validation.minLength ?: "0"),
						"max" to (validation.maxLength ?: "*"),
					)
				}
			}
			RawJson.JsonArray(res)
		}
	}
}
