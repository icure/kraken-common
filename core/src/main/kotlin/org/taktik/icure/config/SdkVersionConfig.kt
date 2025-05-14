package org.taktik.icure.config

import org.springframework.stereotype.Component
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.entities.utils.SemanticVersion

@Component
class SdkVersionConfig(
	private val sessionInformationProvider: SessionInformationProvider
) {

	enum class FeatureLevel(val version: SemanticVersion) {
		AccessLogUserRights(SemanticVersion("1.3.0"))
	}

	/**
	 * Checks if the [featureLevel] passed as parameter should be enabled, based on the optional `Icure-Request-Cardinal-Version`
	 * header that Cardinal SDK may send to the backend.
	 */
	suspend fun hasAtLeastFeatureLevelOf(featureLevel: FeatureLevel): Boolean =
		sessionInformationProvider.getCallerCardinalVersion()?.let {
			it >= featureLevel.version
		} ?: false

}