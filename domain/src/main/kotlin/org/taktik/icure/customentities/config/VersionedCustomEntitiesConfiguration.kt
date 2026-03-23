package org.taktik.icure.customentities.config

import org.taktik.icure.customentities.config.typing.EnumDefinition
import org.taktik.icure.customentities.config.typing.ObjectDefinition
import org.taktik.icure.customentities.config.typing.truncateValueForErrorMessage
import org.taktik.icure.customentities.config.typing.validateIdentifier
import org.taktik.icure.customentities.util.BuiltinDefinitionsProvider
import org.taktik.icure.customentities.util.ExtendableBuiltinEntityValidator
import org.taktik.icure.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.errorreporting.CollectedErrors
import org.taktik.icure.errorreporting.ErrorCollector
import org.taktik.icure.errorreporting.ScopePath
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.errorreporting.appending

data class VersionedCustomEntitiesConfiguration(
	val rev: String? = null,
	val owningGroup: String,
	val version: Int,
	val objects: Map<String, ObjectDefinition>,
	val enums: Map<String, EnumDefinition>,
	// TODO type aliases -> allows to share validation logic in type configs for properties that are logically connected + would generate a specialization on the custom SDK
	val extensions: StandardRootEntitiesExtensionConfig,
	val published: Boolean,
) {
	suspend fun validateDefinition(
		builtinDefinitionsProvider: BuiltinDefinitionsProvider,
		makeBuiltinValidator: (CustomEntityConfigResolutionContext) -> ExtendableBuiltinEntityValidator,
	): CollectedErrors {
		val collector = ErrorCollector.Collecting()
		val scopePath = ScopePath("CustomEntitiesConfiguration")
		val validationContext = ScopedErrorCollector(collector, scopePath)
		val resolutionContext = CustomEntityConfigResolutionContext.ofConfig(this)
		validationContext.appending(".objects") {
			objects.forEach { (name, objDef) ->
				validateIdentifier(validationContext, name)
				validationContext.appending(".", name) {
					objDef.validateDefinition(
						CustomEntityConfigValidationContext(
							resolutionContext,
							validationContext,
							makeBuiltinValidator(resolutionContext),
							builtinDefinitionsProvider
						)
					)
				}
			}
		}
		validationContext.appending(".enums") {
			enums.forEach { (name, objDef) ->
				validateIdentifier(validationContext, name)
				validationContext.appending(".", name) {
					objDef.validateDefinition(validationContext)
				}
			}
		}
		sequenceOf(
			objects.keys,
			enums.keys,
		).flatten().groupingBy { it }.eachCount().forEach { (identifier, occurrences) ->
			if (occurrences > 1) {
				validationContext.addError(
					"GE-CONFIG-DUPID",
					"id" to truncateValueForErrorMessage(identifier),
					"count" to occurrences
				)
			}
		}
		extensions.allDefined.forEach { (krakenName, config) ->
			validationContext.appending(".extensions.", krakenName) {
				val rootDef = objects[config.objectDefinitionReference]
				if (rootDef == null) {
					validationContext.addError(
						"GE-CONFIG-EXT-ROOTREF",
						"ref" to truncateValueForErrorMessage(config.objectDefinitionReference),
						"expectedBaseEntity" to krakenName
					)
				} else if (rootDef.builtinExtension?.entityName != krakenName) {
					validationContext.addError(
						"GE-CONFIG-EXT-ROOTBASE",
						"ref" to truncateValueForErrorMessage(config.objectDefinitionReference),
					)
				}
			}
		}
		return collector.collectedErrors
	}
}