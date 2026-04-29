package org.taktik.icure.config

import org.taktik.icure.entities.utils.SemanticVersion

interface CardinalVersionConfig {

	companion object {
		val minCardinalModelVersion = SemanticVersion("2.0.0")
	}

	suspend fun getUserCardinalVersion(): SemanticVersion?

	suspend fun useLegacyDataModelCompatibility(): Boolean
}