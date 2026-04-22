package org.taktik.icure.customentities.util

import org.taktik.icure.customentities.config.typing.ObjectDefinition
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.services.external.rest.v2.mapper.MappersWithCustomExtensions.MapperExtensionsValidationContextImpl

// Note: for multiplatform we will need to have a different implementation, not based on mappers but only based on
// cardinal models + multiplatform serialization
class MapperBasedExtendableBuiltinEntityValidator(
	private val configsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	private val resolutionContext: CustomEntityConfigResolutionContext,
) : ExtendableBuiltinEntityValidator {

	override fun validateAndMapExtendedBuiltinObjectForStore(
		extendedObjectDefinition: ObjectDefinition,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject =
		requireNotNull(
			configsProvider.objectToDomain[
				checkNotNull(extendedObjectDefinition.builtinExtension?.entityName) {
					"Object definition does not extend a baseEntity"
				}
			]
		) {
			"This version of the cardinal backend does not support builtin objects of type `${extendedObjectDefinition.builtinExtension?.entityName}`, you might need to use a more recent version or switch to a fully custom entity"
		}.invoke(
			value,
			MapperExtensionsValidationContextImpl(
				resolutionContext,
				errorCollector,
				configsProvider,
				extendedObjectDefinition
			),
			errorCollector
		)

	override fun validateAndMapPlainBuiltinObjectForStore(
		entityType: String,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject =
		requireNotNull(configsProvider.objectToDomain[entityType]) {
			"This version of the cardinal backend does not support builtin objects of type `$entityType`, you might need to use a more recent version or switch to a fully custom entity"
		}.invoke(value, null, errorCollector)

	override fun validateAndMapBuiltinEnumForStore(
		enumType: String,
		value: RawJson.JsonString,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonString =
		requireNotNull(configsProvider.enumToDomain[enumType]) {
			"This version of the cardinal backend does not support builtin enums of type `$enumType`, you might need to use a more recent version or switch to a custom enum"
		}.invoke(value, errorCollector)
}