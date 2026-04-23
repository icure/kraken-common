package org.taktik.icure.config

import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.services.external.rest.ModelMappingVersionContext

interface CardinalVersionConfig {

	companion object {
		class ModelMappingVersionContextImpl(
			override val cardinalModelVersion: SemanticVersion?
		) : ModelMappingVersionContext {
			override fun shouldUseCardinalModel(): Boolean =
				cardinalModelVersion != null && cardinalModelVersion >= CardinalModelInfo.minCardinalModelVersion
		}
	}

	suspend fun getUserCardinalVersion(): SemanticVersion?

	suspend fun shouldUseCardinalModel(): Boolean

	suspend fun getMappingContextForCurrentUser(): ModelMappingVersionContext =
		ModelMappingVersionContextImpl(getUserCardinalVersion())
}