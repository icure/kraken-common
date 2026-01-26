package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.appending

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class ListTypeConfig(
	val elementType: GenericTypeConfig,
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null,
) : GenericTypeConfig {
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
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
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
	) {
		validationContext.appending("[*]") {
			elementType.validateConfig(
				resolutionContext,
				validationContext
			)
		}
		validation?.apply {
			if (minLength != null && minLength <= 0) {
				validationContext.addError("Invalid minLength, should be greater than 0")
			}
			if (maxLength != null && maxLength <= 0) {
				validationContext.addError("Invalid maxLength, should be greater than 0")
			}
			if (minLength != null && maxLength != null && maxLength < minLength) {
				validationContext.addError("Invalid length bounds, maxLength should be greater than or equal to minLength")
			}
			if (uniqueValues) {
				if (
					elementType !is StringTypeConfig
						&& elementType !is IntTypeConfig
						&& elementType !is EnumTypeConfig
				) {
					validationContext.addError("Unsupported element type for unique list")
				}
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson,
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonArray) {
			validationContext.addError("Invalid type, expected Array")
			value
		} else {
			val res =
				validationContext.appending("[") {
					value.items.mapIndexed { index, element ->
						validationContext.appending(index, "]") {
							elementType.validateAndMapValueForStore(
								resolutionContext,
								validationContext,
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
					validationContext.addError("Array length out of bounds")
				}
				if(
					validation.uniqueValues && res.toSet().size != res.size
				) {
					validationContext.addError("Duplicate items in unique values array")
				}
			}
			RawJson.JsonArray(res)
		}
	}
}