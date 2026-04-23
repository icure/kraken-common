package org.taktik.icure.services.external.rest

import org.taktik.icure.entities.utils.SemanticVersion

interface ModelMappingVersionContext {
	val cardinalModelVersion: SemanticVersion?
	fun shouldUseCardinalModel(): Boolean

	object Legacy : ModelMappingVersionContext {
		override val cardinalModelVersion: Nothing? get() = null

		override fun shouldUseCardinalModel(): Boolean = false
	}
}