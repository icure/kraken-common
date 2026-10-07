package org.taktik.icure.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("app")
@ConfigurationProperties("icure.errors")
data class ErrorProperties(
    /**
     * If null coded error messages will only contain basic information: faster but worse for debugging.
     */
    var codedErrorMessagesSource: String? = null,
    /**
     * If true when mapping DTO->Domain the controllers will keep track of the path to the entity / property being
     * mapped, in order to provide better error reporting.
     * This comes with a performance cost, so it is disabled by default.
     */
    var reportMappingPath: Boolean? = null
)