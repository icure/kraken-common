package org.taktik.icure.domain.customentities.util

import org.taktik.icure.domain.customentities.config.VersionedCustomEntitiesConfiguration
import org.taktik.icure.domain.customentities.config.typing.EnumDefinition
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.exceptions.IllegalEntityException

interface CustomEntityConfigResolutionContext {
	fun resolveObjectReference(reference: String): ObjectDefinition?
	fun resolveEnumReference(reference: String): EnumDefinition?

	companion object {
		fun ofConfig(
			config: VersionedCustomEntitiesConfiguration
		): CustomEntityConfigResolutionContext = CustomEntityConfigResolutionContextImpl(
			config.objects,
			config.enums
		)

		/**
		 * If at some point we don't have the configuration with everything in it we could have an alternative suspend
		 * initializer, and given a certain ObjectDefinition automatically loads all the definitions needed (directly or
		 * indirectly) by that definition.
		 * This allows keeping resolve methods stay non-suspend.
		 */
	}

	object Empty : CustomEntityConfigResolutionContext {
		override fun resolveObjectReference(reference: String): ObjectDefinition? = null

		override fun resolveEnumReference(reference: String): EnumDefinition? = null
	}
}

private class CustomEntityConfigResolutionContextImpl(
	private val objects: Map<String, ObjectDefinition>,
	private val enums: Map<String, EnumDefinition>
) : CustomEntityConfigResolutionContext {
	override fun resolveObjectReference(reference: String): ObjectDefinition? =
		objects[reference]

	override fun resolveEnumReference(reference: String): EnumDefinition? =
		enums[reference]
}

/**
 * Only to use when using a validated configuration, in case of illegal manipulation of the configuration that causes
 * a mandatory reference to be missing throws an IllegalEntityException.
 * No need to have a ResolutionPath for proper error messages as this should never happen if the configuration was
 * always changed through the appropriate endpoints.
 */
fun CustomEntityConfigResolutionContext.resolveRequiredObjectReference(
	reference: String
): ObjectDefinition = resolveObjectReference(reference)
	?: throw IllegalEntityException("Object definition for reference `$reference` not found")

/**
 * Only to use when using a validated configuration, in case of illegal manipulation of the configuration that causes
 * a mandatory reference to be missing throws an IllegalEntityException.
 * No need to have a ResolutionPath for proper error messages as this should never happen if the configuration was
 * always changed through the appropriate endpoints.
 */
fun CustomEntityConfigResolutionContext.resolveRequiredEnumReference(
	reference: String
): EnumDefinition = resolveEnumReference(reference)
	?: throw IllegalEntityException("Object definition for reference `$reference` not found")
