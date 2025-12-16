package org.taktik.icure.domain.customentities.config.typing

import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

/**
 * A type representing some json that should be stored as is, without any validation.
 * This can be any json value: string, number, boolean, object, array, or null.
 */
data object JsonTypeConfig : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		path: ResolutionPath,
		value: RawJson
	): RawJson =
		value
}