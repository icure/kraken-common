package org.taktik.icure.customentities.util

import org.taktik.icure.customentities.config.typing.ObjectDefinition
import org.taktik.icure.customentities.util.BuiltinDefinitionsProvider
import org.taktik.icure.customentities.util.ExtendableBuiltinEntityValidator
import org.taktik.icure.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.services.external.rest.v2.mapper.MappersWithCustomExtensions.MapperExtensionsValidationContextImpl

class MapperBasedExtendableBuiltinEntityValidator(
	private val configsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	private val resolutionContext: CustomEntityConfigResolutionContext,
	private val builtinDefinitions: BuiltinDefinitionsProvider,
) : ExtendableBuiltinEntityValidator {

	override fun validateAndMapExtendedBuiltinForStore(
		extendedObjectDefinition: ObjectDefinition,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject =
		checkNotNull(
			configsProvider.configs[
				checkNotNull(extendedObjectDefinition.builtinExtension?.entityName) {
					"Object definition does not extend a baseEntity"
				}
			]
		) {
			"Missing builtin validator for ${extendedObjectDefinition.builtinExtension?.entityName} (extended)"
		}.invoke(
			value,
			MapperExtensionsValidationContextImpl(
				resolutionContext,
				errorCollector,
				builtinDefinitions,
				configsProvider,
				extendedObjectDefinition
			),
			errorCollector
		)

	override fun validateAndMapPlainBuiltinForStore(
		entityType: String,
		value: RawJson.JsonObject,
		errorCollector: ScopedErrorCollector
	): RawJson.JsonObject =
		checkNotNull(configsProvider.configs[entityType]) {
			"Missing builtin validator for $entityType (plain)"
		}.invoke(value, null, errorCollector)
}