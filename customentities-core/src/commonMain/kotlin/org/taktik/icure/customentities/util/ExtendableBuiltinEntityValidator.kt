package org.taktik.icure.customentities.util

import org.taktik.icure.customentities.config.typing.ObjectDefinition
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector

interface ExtendableBuiltinEntityValidator {
	fun validateAndMapExtendedBuiltinObjectForStore(
		extendedObjectDefinition: ObjectDefinition,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject

	fun validateAndMapPlainBuiltinObjectForStore(
		entityType: String,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject

	fun validateAndMapBuiltinEnumForStore(
		enumType: String,
		value: RawJson.JsonString,
		errorCollector: ScopedErrorCollector,
	): RawJson.JsonString
}