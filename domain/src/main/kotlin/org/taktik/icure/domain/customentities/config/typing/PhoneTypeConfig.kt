package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.utils.Validation

/**
 * Type for phone number fields.
 * The phone must be in international format and except for the starting `+` can't contain any non-digit character.
 */
data class PhoneTypeConfig(
	val nullable: Boolean = false,
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: JsonNode
	): JsonNode = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is TextNode) {
			"$path: invalid type, expected Text (phone)"
		}
		require(Validation.validPhone(value.asText())) {
			"$path: invalid value for phone"
		}
		value
	}
}
