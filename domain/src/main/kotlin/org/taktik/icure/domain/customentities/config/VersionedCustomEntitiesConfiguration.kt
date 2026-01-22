package org.taktik.icure.domain.customentities.config

import org.taktik.icure.domain.customentities.config.typing.EnumDefinition
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.domain.customentities.config.typing.truncateValueForErrorMessage
import org.taktik.icure.domain.customentities.config.typing.validateIdentifier
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath

data class VersionedCustomEntitiesConfiguration(
	val rev: String? = null,
	val owningGroup: String,
	val version: Int,
	val objects: Map<String, ObjectDefinition>,
	val enums: Map<String, EnumDefinition>,
	// TODO type aliases -> allows to share validation logic in type configs for properties that are logically connected + would generate a specialization on the custom SDK
	val extensions: ExtensionsConfiguration,
	val published: Boolean,
) {
	// TODO replace ResolutionPath with an error collector
	// - Interface can take both warnings and errors
	// - One implementation throws on first error and ignores warning
	// - Other implementation just collects everything
	// - Errors should be data classes with constant code, and parameters like path, input, ..., to allow for multilingual message later

	suspend fun validateDefinition() {
		val path = ResolutionPath()
		val context = CustomEntityConfigResolutionContext.ofConfig(this)
		path.appending("objects") {
			objects.forEach { (name, objDef) ->
				validateIdentifier(path, name)
				path.appending(".", name) {
					objDef.validateDefinition(context, path)
				}
			}
		}
		path.appending("enums") {
			enums.forEach { (name, objDef) ->
				validateIdentifier(path, name)
				path.appending(".", name) {
					objDef.validateDefinition(path)
				}
			}
		}
		extensions.allDefined.forEach { (krakenName, config) ->
			path.appending("extensions.", krakenName) {
				require(config.objectDefinitionReference in objects) {
					"$path: invalid root extension, object definition `${truncateValueForErrorMessage(config.objectDefinitionReference)}` not found"
				}
				config.embeddedEntitiesConfigs.forEach { (className, objDefRef) ->
					require(objDefRef in objects) {
						"$path: invalid extension for ${className.take(128)}, object definition `${truncateValueForErrorMessage(config.objectDefinitionReference)}` not found"
					}
					// TODO validate that className is actually embedded in the entity or in custom extensions definitions with BuiltinTypeConfig.
				}
			}
		}
	}
}