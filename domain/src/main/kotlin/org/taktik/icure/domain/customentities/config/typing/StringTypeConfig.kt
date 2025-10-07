package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

class StringTypeConfig(
	val nullable: Boolean = false,
	val validation: ValidationConfig? = null
) : GenericTypeConfig {
	data class ValidationConfig(
		val minLength: Int? = null,
		val maxLength: Int? = null,
	)

	override fun validateConfig(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath
	) {
		validation?.apply {
			require(minLength != null || maxLength != null) {
				"$path: invalid string validation config, must specify a length range"
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: JsonNode
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is TextNode) {
			"$path: invalid type, expected Text (string)"
		}
		if (validation != null) {
			require(
				(validation.minLength == null || value.asText().length >= validation.minLength)
					&& (validation.maxLength == null || value.asText().length <= validation.maxLength)
			) {
				"$path: string length out of bounds"
			}
		}
		value
	}
}