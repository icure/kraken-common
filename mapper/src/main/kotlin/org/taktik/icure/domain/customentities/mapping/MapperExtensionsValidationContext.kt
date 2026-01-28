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
	additionalImports = ["org.taktik.icure.errorreporting.appending"]
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