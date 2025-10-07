package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.taktik.icure.utils.Validation

/**
 * Type for email fields.
 */
data class EmailTypeConfig(
	val nullable: Boolean = false,
) : org.taktik.icure.domain.customentities.config.typing.GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext,
		path: org.taktik.icure.domain.customentities.util.ResolutionPath,
		value: JsonNode
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is TextNode) {
			"$path: invalid type, expected Text (email)"
		}
		require(Validation.validEmail(value.asText())) {
			"$path: invalid value for email"
		}
		value
	}
}
