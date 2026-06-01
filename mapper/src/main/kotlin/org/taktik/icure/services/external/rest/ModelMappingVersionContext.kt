package org.taktik.icure.services.external.rest

import org.taktik.icure.entities.utils.SemanticVersion

interface ModelMappingVersionContext {
	val cardinalModelVersion: SemanticVersion?
	fun useLegacyDataModelCompatibility(): Boolean

	object Legacy : ModelMappingVersionContext {
		override val cardinalModelVersion: Nothing? get() = null

		override fun useLegacyDataModelCompatibility(): Boolean = true
	}
}