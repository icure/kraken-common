package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.taktik.icure.entities.RawJson
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
			require(minLength == null || minLength > 0) {
				"$path: invalid minLength, should be greater than 0"
			}
			require(maxLength == null || maxLength > 0) {
				"$path: invalid maxLength, should be greater than 0"
			}
			require(minLength == null || maxLength == null || maxLength >= minLength) {
				"$path: invalid length bounds, maxLength should be greater than or equal to minLength"
			}
		}
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonString) {
			"$path: invalid type, expected Text (string)"
		}
		if (validation != null) {
			require(
				(validation.minLength == null || value.value.length >= validation.minLength)
					&& (validation.maxLength == null || value.value.length <= validation.maxLength)
			) {
				"$path: string length out of bounds"
			}
		}
		value
	}
}