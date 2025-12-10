package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

data class ListTypeConfig(
	val elementType: GenericTypeConfig,
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null,
) : GenericTypeConfig {

	data class ValidationConfig(
		val minLength: Int? = null,
		val maxLength: Int? = null,
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
		value: JsonNode,
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is ArrayNode) {
			"$path: invalid type, expected Array"
		}
		val res = value.mapIndexed { index, element ->
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
		ArrayNode(JsonNodeFactory.instance, res)
	}

	override fun mapValueForRead(
		resolutionContext: CustomEntityConfigResolutionContext,
		value: JsonNode
	): JsonNode =
		if (value is ArrayNode && elementType.shouldMapForRead) {
			ArrayNode(
				JsonNodeFactory.instance,
				value.map { item ->
					elementType.mapValueForRead(
						resolutionContext,
						item
					)
				}
			)
		} else {
			value
		}

	override val shouldMapForRead: Boolean @JsonIgnore get() = true
}