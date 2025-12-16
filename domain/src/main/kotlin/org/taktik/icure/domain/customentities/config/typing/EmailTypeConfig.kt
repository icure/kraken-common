package org.taktik.icure.domain.customentities.config.typing

import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.utils.Validation

/**
 * Type for email fields.
 */
data class EmailTypeConfig(
	val nullable: Boolean = false,
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson = validatingAndIgnoringNullForStore(path, value, nullable) {
		require(value is RawJson.JsonString) {
			"$path: invalid type, expected Text (email)"
		}
		require(Validation.validEmail(value.value)) {
			"$path: invalid value for email"
		}
		value
	}
}
