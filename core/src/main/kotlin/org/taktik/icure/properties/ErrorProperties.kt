package org.taktik.icure.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile

@Profile("app")
@ConfigurationProperties("icure.errors")
class ErrorProperties(
	/**
	 * If null coded error messages will only contain basic information: faster but worse for debugging.
	 */
	val codedErrorMessagesSource: String? = null,
	/**
	 * If true when mapping DTO->Domain the controllers will keep track of the path to the entity / property being
	 * mapped, in order to provide better error reporting.
	 * This comes with a performance cost, so it is disabled by default.
	 */
	val reportMappingPath: Boolean? = null
)