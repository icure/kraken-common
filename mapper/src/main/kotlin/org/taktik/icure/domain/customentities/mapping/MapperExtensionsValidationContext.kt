package org.taktik.icure.domain.customentities.mapping

import org.taktik.icure.entities.RawJson
import org.taktik.icure.entities.embed.Extendable
import org.taktik.icure.entities.embed.ExtendableRoot
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableRootDto

interface MapperExtensionsValidationContext {
	fun validateAndMapRootExtensionsForStore(
		entity: ExtendableRootDto
	): RawJson.JsonObject?

	fun validateAndMapEmbeddedExtensionsForStore(
		entity: ExtendableDto,
		entityCanonicalName: String,
	): RawJson.JsonObject?

	object Empty : MapperExtensionsValidationContext {
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