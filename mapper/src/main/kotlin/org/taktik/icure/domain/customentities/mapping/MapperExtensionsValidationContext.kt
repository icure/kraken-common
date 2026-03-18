package org.taktik.icure.domain.customentities.mapping

import org.mapstruct.MappingContextCollector
import org.taktik.icure.domain.customentities.config.ExtendableEntityName
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableRootDto

// TODO Update annotation
@MappingContextCollector(
	beforeEnteringProperty = "%X.withProperty(%P)·{",
	afterExitingProperty = "}",
	beforeEnteringListItem = "%X.withListItem(%I)·{",
	afterExitingListItem = "}",
	beforeEnteringMapEntry = "%X.withMapEntry(%K)·{",
	afterExitingMapEntry = "}",
	additionalImports = [
		"org.taktik.icure.domain.customentities.mapping.withProperty",
		"org.taktik.icure.domain.customentities.mapping.withListItem",
		"org.taktik.icure.domain.customentities.mapping.withMapEntry"
	],
	invokeOnlyOnExplicitRequirement = true
)
interface MapperExtensionsValidationContext {
	/**
	 * May mutate this, implementation dependent
	 */
	fun enterProperty(propertyName: String)

	/**
	 * May mutate this, implementation dependent
	 */
	fun exitProperty()

	/**
	 * May mutate this, implementation dependent
	 */
	fun enterListItem(index: Int)

	/**
	 * May mutate this, implementation dependent
	 */
	fun exitListItem()

	/**
	 * May mutate this, implementation dependent
	 */
	fun enterMapEntry(key: Any)

	/**
	 * May mutate this, implementation dependent
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