package org.taktik.icure.config

import org.taktik.icure.FilteringVersions
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

	/**
	 * If true the user is using a version of Cardinal that doesn't include the deleted attachments in the dtos.
	 * See [org.taktik.icure.entities.base.HasDataAttachments.deletedAttachments].
	 */
	suspend fun deletedAttachmentsMayBeOmitted(): Boolean =
		getUserCardinalVersion()?.let {
			it >= SemanticVersion(FilteringVersions.GENERIC_ENTITIES_CARDINAL_MIN_VERSION)
		} == true

	suspend fun getMappingContextForCurrentUser(): ModelMappingVersionContext =
		ModelMappingVersionContextImpl(
			getUserCardinalVersion(),
			forceLegacyDataModelCompatibility()
		)
}