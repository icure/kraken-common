package org.taktik.icure.domain.customentities.util

import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.entities.RawJson

interface ExtendableBuiltinEntityValidator {
	fun validateAndMapExtendedBuiltinForStore(
		extendedObjectDefinition: ObjectDefinition,
		value: RawJson.JsonObject,
	): RawJson.JsonObject
}