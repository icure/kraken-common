package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
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
			if (minLength != null && minLength < 0) {
				validationContext.addError("GE-LIST-MIN")
			}
			if (maxLength != null && maxLength < 0) {
				validationContext.addError("GE-LIST-MAX")
			}
			if (minLength != null && maxLength != null && maxLength < minLength) {
				validationContext.addError("GE-LIST-NORANGE")
			} else if (maxLength == 0) {
				validationContext.addWarning("GE-LIST-WEMPTY")
			}
			if (uniqueValues) {
				if (
					elementType !is StringTypeConfig
						&& elementType !is IntTypeConfig
						&& elementType !is EnumTypeConfig
				) {
					validationContext.addError("GE-LIST-UNIQUETYPE")
				}
			}
			if (minLength == 0) {
				validationContext.addWarning("GE-LIST-WMIN")
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson,
	): RawJson = validatingAndIgnoringNullForStore(validationContext, value, nullable) {
		if (value !is RawJson.JsonArray) {
			validationContext.addError("GE-LIST-JSON")
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
					validationContext.addError(
						"GE-LIST-OUTRANGE",
						"length" to res.size,
						"min" to (validation.minLength ?: "0"),
						"max" to (validation.maxLength ?: "*"),
					)
				}
				if(
					validation.uniqueValues && res.toSet().size != res.size
				) {
					validationContext.addError("GE-LIST-DUPLICATES")
				}
			}
			RawJson.JsonArray(res)
		}
	}
}