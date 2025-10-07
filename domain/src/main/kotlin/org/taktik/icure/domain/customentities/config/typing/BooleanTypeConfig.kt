package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BooleanNode

/**
 * Represents a configuration for a boolean type.
 */
data class BooleanTypeConfig(
	val nullable: Boolean = false,
) : org.taktik.icure.domain.customentities.config.typing.GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext,
		path: org.taktik.icure.domain.customentities.util.ResolutionPath,
		value: JsonNode
	): JsonNode =
		_root_ide_package_.org.taktik.icure.domain.customentities.config.typing.validatingAndIgnoringNullForStore(
			path,
			value,
			nullable
		) {
			require(value is BooleanNode) {
				"$path: invalid type, expected Boolean"
			}
			value
		}
}