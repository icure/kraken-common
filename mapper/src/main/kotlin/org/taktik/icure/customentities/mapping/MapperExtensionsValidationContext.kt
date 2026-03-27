package org.taktik.icure.customentities.mapping

import org.mapstruct.MappingContextCollector
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector

/**
 * Keep track of the context used for mapping entities that may have extension properties.
 *
 * The context includes two types of information:
 * - The rules for validating and mapping the extensions on the entity being mapped (including embedded entities
 *   extensions)
 * - An optional path to the current position in the entity being mapped, that can be used for detailed error reporting.
 *
 * Depending on the implementation, the various enter/exit methods may mutate the context.
 */
@MappingContextCollector(
	beforeEnteringProperty = "%X.withProperty(%P)·{",
	afterExitingProperty = "}",
	beforeEnteringListItem = "%X.withListItem(%I)·{",
	afterExitingListItem = "}",
	beforeEnteringMapEntry = "%X.withMapEntry(%K)·{",
	afterExitingMapEntry = "}",
	additionalImports = [
		"org.taktik.icure.customentities.mapping.withProperty",
		"org.taktik.icure.customentities.mapping.withListItem",
		"org.taktik.icure.customentities.mapping.withMapEntry",
	],
	invokeOnlyOnExplicitRequirement = true
)
interface MapperExtensionsValidationContext {
	/**
	 * Update the extension validation context AND the path context if present to include the property name.
	 * This method is unsuited for usage with the [org.taktik.icure.entities.embed.Extendable.extensions] property, you
	 * should instead use the [validateAndMapCurrentExtension] method directly on the context.
	 */
	fun enterProperty(propertyName: String)

	/**
	 * Update the extension validation context AND the path context if present to remove the last entered property name.
	 * The behavior is undefined if the last entered context was not a property name.
	 */
	fun exitProperty()

	/**
	 * Update the path context if present to include the list item index.
	 */
	fun enterListItem(index: Int)

	/**
	 * Update the path context if present to remove the last entered list item index.
	 * The behavior is undefined if the last entered context was not a list item index.
	 */
	fun exitListItem()

	/**
	 * Update the path context if present to include the map key.
	 */
	fun enterMapEntry(key: Any)

	/**
	 * Update the path context if present to remove the last entered map key.
	 * The behavior is undefined if the last entered context was not a map key.
	 */
	fun exitMapEntry()

	fun validateAndMapCurrentExtension(extensionValue: RawJson.JsonObject?): RawJson.JsonObject?

	/**
	 * A no-op implementation of [MapperExtensionsValidationContext] that enforces absence of extensions.
	 *
	 * # MUST BE STATELESS
	 *
	 * This value is also used as a default value for the mapping context in some mappers.
	 *
	 * If at some point we need to add state (and make this into a class), we should also change the way mappers
	 * are generated.
	 *
	 * In particular, instead of having a shared default value instance in each mapper, we should have a factory method
	 * in the mapper, and create a new instance for each invocation of the mapping function.
	 */
	object Empty : MapperExtensionsValidationContext {
		override fun enterProperty(propertyName: String) {
			// No op
		}

		override fun exitProperty() {
			// No op
		}

		override fun enterListItem(index: Int) {
			// No op
		}

		override fun exitListItem() {
			// No op
		}

		override fun enterMapEntry(key: Any) {
			// No op
		}

		override fun exitMapEntry() {
			// No op
		}

		override fun validateAndMapCurrentExtension(extensionValue: RawJson.JsonObject?): RawJson.JsonObject? {
			require(extensionValue == null) { "Extensions are not enabled for this entity" }
			return null
		}
	}

	class NotAllowed(
		val errorCollector: ScopedErrorCollector,
	): MapperExtensionsValidationContext {
		override fun enterProperty(propertyName: String) {
			// No op
		}

		override fun exitProperty() {
			// No op
		}

		override fun enterListItem(index: Int) {
			// No op
		}

		override fun exitListItem() {
			// No op
		}

		override fun enterMapEntry(key: Any) {
			// No op
		}

		override fun exitMapEntry() {
			// No op
		}

		override fun validateAndMapCurrentExtension(extensionValue: RawJson.JsonObject?): RawJson.JsonObject? {
			if (extensionValue != null) {
				errorCollector.addError("GE-OBJECT-EXTENSIONS")
			}
			return null
		}
	}
}

inline fun <T> MapperExtensionsValidationContext.withProperty(propertyName: String, block: () -> T): T {
	enterProperty(propertyName)
	return try {
		block()
	} finally {
		exitProperty()
	}
}

inline fun <T> MapperExtensionsValidationContext.withListItem(index: Int, block: () -> T): T {
	enterListItem(index)
	return try {
		block()
	} finally {
		exitListItem()
	}
}

inline fun <T> MapperExtensionsValidationContext.withMapEntry(key: Any, block: () -> T): T {
	enterMapEntry(key)
	return try {
		block()
	} finally {
		exitMapEntry()
	}
}