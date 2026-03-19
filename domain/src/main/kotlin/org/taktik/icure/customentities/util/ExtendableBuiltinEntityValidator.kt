package org.taktik.icure.customentities.util

import org.taktik.icure.customentities.config.typing.ObjectDefinition
import org.taktik.icure.entities.RawJson

interface ExtendableBuiltinEntityValidator {
	fun validateAndMapExtendedBuiltinForStore(
		extendedObjectDefinition: ObjectDefinition,
		value: RawJson.JsonObject,
	): RawJson.JsonObject
}