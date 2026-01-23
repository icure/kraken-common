package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

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
		path: ResolutionPath
	) {
		path.appending("[x]") {
			elementType.validateConfig(
				resolutionContext,
				path
			)
		}
		validation?.apply {
			require(minLength == null || minLength > 0) {
				"$path: invalid minLength, should be greater than 0"
			}
			require(maxLength == null || maxLength > 0) {
				"$path: invalid maxLength, should be greater than 0"
			}
			require(minLength == null || maxLength == null || maxLength >= minLength) {
				"$path: invalid length bounds, maxLength should be greater than or equal to minLength"
			}
			if (uniqueValues) {
				require(
					elementType is StringTypeConfig
						|| elementType is IntTypeConfig
						|| elementType is EnumTypeConfig
				) {
					"$path: unsupported element type for unique list"
				}
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson,
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonArray) {
			"$path: invalid type, expected Array"
		}
		val res = value.items.mapIndexed { index, element ->
			path.appending("[$index]") {
				elementType.validateAndMapValueForStore(
					resolutionContext,
					path,
					element
				)
			}
		}
		if (validation != null) {
			require(
				(validation.minLength == null || res.size >= validation.minLength)
					&& (validation.maxLength == null || res.size <= validation.maxLength)
			) {
				"$path: array length out of bounds"
			}
			require(
				!validation.uniqueValues || res.toSet().size == res.size
			) {
				"$path: duplicate items in unique values array"
			}
		}
		RawJson.JsonArray(res)
	}
}