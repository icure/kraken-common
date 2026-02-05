package org.taktik.icure.domain.customentities.config

import org.taktik.icure.domain.customentities.config.typing.EnumDefinition
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.domain.customentities.config.typing.truncateValueForErrorMessage
import org.taktik.icure.domain.customentities.config.typing.validateIdentifier
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
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
	suspend fun validateDefinition(): CollectedErrors {
		val collector = ErrorCollector.Collecting()
		val validationContext = ScopedErrorCollector(collector, ScopePath("CustomEntitiesConfiguration"))
		val resolutionContext = CustomEntityConfigResolutionContext.ofConfig(this)
		validationContext.appending(".objects") {
			objects.forEach { (name, objDef) ->
				validateIdentifier(validationContext, name)
				validationContext.appending(".", name) {
					objDef.validateDefinition(resolutionContext, validationContext)
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
				if (config.objectDefinitionReference !in objects) {
					validationContext.addError(
						"GE-CONFIG-EXT-ROOT",
						"ref" to truncateValueForErrorMessage(config.objectDefinitionReference)
					)
				}
				config.embeddedEntitiesConfigs.forEach { (className, objDefRef) ->
					if (objDefRef !in objects) {
						validationContext.addError(
							"GE-CONFIG-EXT-EMBEDDED",
							"ref" to truncateValueForErrorMessage(objDefRef),
							"className" to truncateValueForErrorMessage(className, 128)
						)
					}
					// TODO validate that className is actually embedded in the entity or in custom extensions definitions with BuiltinTypeConfig.
				}
			}
		}
		return collector.collectedErrors
	}
}