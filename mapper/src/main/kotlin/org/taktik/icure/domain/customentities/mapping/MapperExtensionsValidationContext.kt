package org.taktik.icure.domain.customentities.mapping

import org.mapstruct.MappingContextCollector
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableRootDto

@MappingContextCollector(
	beforeEnteringProperty = "%X.collector.appending(\".\", %P)·{",
	afterExitingProperty = "}",
	beforeEnteringListItem = "%X.collector.appending(\"[\", %I, \"]\")·{",
	afterExitingListItem = "}",
	beforeEnteringMapEntry = "%X.collector.appending(\"{\", %K, \"}\")·{",
	afterExitingMapEntry = "}",
	additionalImports = ["org.taktik.icure.errorreporting.appending"],
	invokeOnlyOnExplicitRequirement = true
)
interface MapperExtensionsValidationContext {
	val collector: ScopedErrorCollector?

	fun validateAndMapRootExtensionsForStore(
		entity: ExtendableRootDto
	): RawJson.JsonObject?

	fun validateAndMapEmbeddedExtensionsForStore(
		entity: ExtendableDto,
		entityCanonicalName: String,
	): RawJson.JsonObject?

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
		override val collector: ScopedErrorCollector? = null

		override fun validateAndMapRootExtensionsForStore(entity: ExtendableRootDto): RawJson.JsonObject? {
			require(entity.extensions == null) { "Extensions are not enabled on ${entity::class.simpleName}" }
			return null
		}

		override fun validateAndMapEmbeddedExtensionsForStore(
			entity: ExtendableDto,
			entityCanonicalName: String
		): RawJson.JsonObject? {
			require(entity.extensions == null) { "Extensions are not enabled on $entityCanonicalName" }
			return null
		}
	}
}