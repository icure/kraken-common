package org.taktik.icure.domain.customentities.util

import org.taktik.icure.domain.customentities.config.ExtendableEntityName
import org.taktik.icure.entities.RawJson

interface CustomEntityBuiltinValidator {
	fun validateAndMapExtendableBuiltinForStore(
		extendableEntityName: ExtendableEntityName,
		context: CustomEntityConfigValidationContext,
		value: RawJson.JsonObject,
	): RawJson.JsonObject
}