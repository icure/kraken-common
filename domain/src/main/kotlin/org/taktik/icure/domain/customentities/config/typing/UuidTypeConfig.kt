package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

/**
 * Type for UUID (not necessarily v4 UUID).
 * Requires proper formatting, and allows only lowercase a-f digits.
 */
data class UuidTypeConfig(
	val nullable: Boolean = false,
) : GenericTypeConfig {
	companion object {
		private val VALIDATION_REGEX = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
	}

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonString) {
			"$path: invalid type, expected Text (UUID)"
		}
		require(VALIDATION_REGEX.matches(value.value)) {
			"$path: invalid value for UUID"
		}
		value
	}
}
