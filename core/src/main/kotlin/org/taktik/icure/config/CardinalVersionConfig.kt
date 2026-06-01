package org.taktik.icure.config

import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.services.external.rest.ModelMappingVersionContext

interface CardinalVersionConfig {

	companion object {
		class ModelMappingVersionContextImpl(
			override val cardinalModelVersion: SemanticVersion?,
			private val forceLegacyDataModelCompatibility: Boolean,
		) : ModelMappingVersionContext {
			override fun useLegacyDataModelCompatibility(): Boolean =
				forceLegacyDataModelCompatibility
					|| cardinalModelVersion == null
					|| cardinalModelVersion < CardinalModelInfo.minCardinalModelVersion
		}
	}

	suspend fun getUserCardinalVersion(): SemanticVersion?

	suspend fun forceLegacyDataModelCompatibility(): Boolean

	suspend fun getMappingContextForCurrentUser(): ModelMappingVersionContext =
		ModelMappingVersionContextImpl(
			getUserCardinalVersion(),
			forceLegacyDataModelCompatibility()
		)
}