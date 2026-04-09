package org.taktik.icure.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("app")
@ConfigurationProperties("icure.observability")
final data class ObservabilityProperties(
	/**
	 * If true enables collection of individual filters timing execution (if desc is set) and inclusion in the response headers.
	 */
	var filterTiming: Boolean = true,
)
