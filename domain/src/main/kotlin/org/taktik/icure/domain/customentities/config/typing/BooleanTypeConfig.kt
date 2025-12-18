package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

/**
 * Represents a configuration for a boolean type.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class BooleanTypeConfig(
	val nullable: Boolean = false,
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson =
		validatingAndIgnoringNullForStore(
			path,
			value,
			nullable
		) {
			require(value is RawJson.JsonBoolean) {
				"$path: invalid type, expected Boolean"
			}
			value
		}
}