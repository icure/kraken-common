package org.taktik.icure.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("app")
@ConfigurationProperties(prefix = "icure.auth.jwt")
data class JwtAuthProperties(
	var expirationMillis: Long = 3600000,
	var refreshExpirationMillis: Long = 86400000
)
