package org.taktik.icure.customentities.util

import org.taktik.icure.customentities.config.typing.ObjectDefinition
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector

interface ExtendableBuiltinEntityValidator {
	fun validateAndMapExtendedBuiltinForStore(
		extendedObjectDefinition: ObjectDefinition,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject

	fun validateAndMapPlainBuiltinForStore(
		entityType: String,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject
}