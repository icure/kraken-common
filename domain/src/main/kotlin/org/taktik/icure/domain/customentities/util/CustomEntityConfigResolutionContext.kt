package org.taktik.icure.domain.customentities.util

import org.taktik.icure.domain.customentities.config.VersionedCustomEntitiesConfiguration
import org.taktik.icure.domain.customentities.config.typing.EnumDefinition
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition

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