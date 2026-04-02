package org.taktik.icure.customentities.util

import org.taktik.icure.customentities.config.typing.GenericTypeConfig
import org.taktik.icure.customentities.config.typing.ObjectDefinition

interface BuiltinDefinitionsProvider {
	fun getBuiltinEnumDefinition(name: String): BuiltinEnumDefinition?
	fun getBuiltinObjectDefinition(name: String): BuiltinObjectDefinition?

	data class BuiltinEnumDefinition(
		val entries: Set<String>
	)

	data class BuiltinObjectDefinition(
		val properties: Map<String, ObjectDefinition.PropertyConfiguration>,
		val isExtendable: Boolean,
		val isRoot: Boolean,
		val deprecatedProperties: Set<String>
	)
}

/**
 * Only to use when using a validated configuration, in case of illegal manipulation of the configuration that causes
 * a mandatory reference to be missing throws an IllegalEntityException.
 * No need to have a ResolutionPath for proper error messages as this should never happen if the configuration was
 * always changed through the appropriate endpoints.
 */
fun BuiltinDefinitionsProvider.getRequiredObjectDefinition(
	reference: String
): BuiltinDefinitionsProvider.BuiltinObjectDefinition = checkNotNull(getBuiltinObjectDefinition(reference)) {
	"Object definition for reference `$reference` not found"
}

/**
 * Only to use when using a validated configuration, in case of illegal manipulation of the configuration that causes
 * a mandatory reference to be missing throws an IllegalEntityException.
 * No need to have a ResolutionPath for proper error messages as this should never happen if the configuration was
 * always changed through the appropriate endpoints.
 */
fun BuiltinDefinitionsProvider.getRequiredEnumDefinition(
	reference: String
): BuiltinDefinitionsProvider.BuiltinEnumDefinition = checkNotNull(getBuiltinEnumDefinition(reference)) {
	"Object definition for reference `$reference` not found"
}