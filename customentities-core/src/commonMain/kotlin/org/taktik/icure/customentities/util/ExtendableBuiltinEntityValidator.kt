package org.taktik.icure.customentities.util

import org.taktik.icure.customentities.config.typing.ObjectDefinition
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector

/**
 * Validate a builtin entity, builtin enum, or custom extension on a builtin entity that is used in a custom entity
 * or custom extension on a builtin entity.
 * This is not used to validate extensions on root entities and extended builtin properties as those are validated
 * directly by the mappers.
 */
/*
 * Since we are currently not allowing to use builtin entities in custom objects / extensions, these methods are
 * actually unused.
 * The reason we are not allowing it is the complexity of properly handling the mapping to domain on write and back to
 * dto on read of the builtin entities when there is potential migrations involved.
 * In the future we might want to allow it again after a potential overhaul with a map for store and a map for read
 * method in that case the general architecture should be reusable.
 */
interface ExtendableBuiltinEntityValidator {
	fun validateAndMapExtendedBuiltinObject(
		extendedObjectDefinition: ObjectDefinition,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject

	fun validateAndMapPlainBuiltinObject(
		entityType: String,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject

	fun validateAndMapBuiltinEnum(
		enumType: String,
		value: RawJson.JsonString,
		errorCollector: ScopedErrorCollector,
	): RawJson.JsonString
}