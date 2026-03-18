package org.taktik.icure.customentities.util

import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.domain.customentities.util.BuiltinDefinitionsProvider
import org.taktik.icure.domain.customentities.util.ExtendableBuiltinEntityValidator
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopePath
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.services.external.rest.v2.mapper.MappersWithCustomExtensions.MapperExtensionsValidationContextImpl

class MapperBasedExtendableBuiltinEntityValidator(
	private val configsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
	private val resolutionContext: CustomEntityConfigResolutionContext,
	private val errorCollector: ScopedErrorCollector,
	private val builtinDefinitions: BuiltinDefinitionsProvider,
) : ExtendableBuiltinEntityValidator {

	override fun validateAndMapExtendedBuiltinForStore(
		extendedObjectDefinition: ObjectDefinition,
		value: RawJson.JsonObject
	): RawJson.JsonObject =
		checkNotNull(
			configsProvider.configs[
				checkNotNull(extendedObjectDefinition.baseEntity) {
					"Object definition does not extend a baseEntity"
				}
			]
		) {
			"Missing builtin validator for ${extendedObjectDefinition.baseEntity}"
		}.invoke(
			value,
			MapperExtensionsValidationContextImpl(
				resolutionContext,
				errorCollector,
				builtinDefinitions,
				configsProvider,
				extendedObjectDefinition
			)
		)
}