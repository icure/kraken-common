package org.taktik.icure.customentities.config.typing

import org.taktik.icure.jackson.annotations.JsonIgnore
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.Include
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.appending

@JsonInclude(Include.NON_DEFAULT)
data class ListTypeConfig(
	val elementType: GenericTypeConfig,
	override val nullable: Boolean = false,
	val validation: ValidationConfig? = null,
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is ListTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	@get:JsonIgnore
	override val objectDefinitionDependencies: Set<Pair<String, Boolean>> get() =
		elementType.objectDefinitionDependencies

	@get:JsonIgnore
	override val enumDefinitionDependencies: Set<Pair<String, Boolean>> get() =
		elementType.enumDefinitionDependencies

	@JsonInclude(Include.NON_DEFAULT)
	data class ValidationConfig(
		val minLength: Int? = null,
		val maxLength: Int? = null,
		/**
		 * Can only be true if the elementType is StringTypeConfig, IntTypeConfig or EnumTypeConfig.
		 * If true, a list with duplicate values will be rejected.
		 *
		 * ## Still ordered
		 *
		 * Unlike a set, the order of elements is preserved and can be meaningful.
		 *
		 * If an object has an entry `"l"` for a list with unique values and default value `["a", "b"]` not stored
		 * explicitly, then `{ "l": ["a", "b"] }` will be stored as `{}`, but `{ "l": ["b", "a"] }` will be stored as
		 * is.
		 *
		 */
		val uniqueValues: Boolean = false
	)

	override fun validateConfig(
		context: CustomEntityConfigValidationContext,
	) {
		context.validation.appending("[*]") {
			elementType.validateConfig(context)
		}
		validation?.apply {
			if (minLength != null && minLength < 0) {
				context.validation.addError("GE-LIST-MIN")
			}
			if (maxLength != null && maxLength < 0) {
				context.validation.addError("GE-LIST-MAX")
			}
			if (minLength != null && maxLength != null && maxLength < minLength) {
				context.validation.addError("GE-LIST-NORANGE")
			} else if (maxLength == 0) {
				context.validation.addWarning("GE-LIST-WEMPTY")
			}
			if (uniqueValues) {
				if (
					elementType !is StringTypeConfig
						&& elementType !is IntTypeConfig
						&& elementType !is EnumTypeConfig
				) {
					context.validation.addError("GE-LIST-UNIQUETYPE")
				}
			}
			if (minLength == 0) {
				context.validation.addWarning("GE-LIST-WMIN")
			}
		}
	}

	override fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson,
	): RawJson = validatingNullForStore(context.validation, value, nullable) {
		if (value !is RawJson.JsonArray) {
			context.validation.addError("GE-LIST-JSON")
			value
		} else {
			val res =
				context.validation.appending("[") {
					value.items.mapIndexed { index, element ->
						context.validation.appending(index, "]") {
							elementType.validateAndMapValueForStore(
								context,
								element
							)
						}
					}
				}
			if (validation != null) {
				if (
					(validation.minLength != null && res.size < validation.minLength)
					|| (validation.maxLength != null && res.size > validation.maxLength)
				) {
					context.validation.addError(
						"GE-LIST-OUTRANGE",
						"length" to res.size,
						"min" to (validation.minLength ?: "0"),
						"max" to (validation.maxLength ?: "*"),
					)
				}
				if(
					validation.uniqueValues && res.toSet().size != res.size
				) {
					context.validation.addError("GE-LIST-DUPLICATES")
				}
			}
			RawJson.JsonArray(res)
		}
	}
}