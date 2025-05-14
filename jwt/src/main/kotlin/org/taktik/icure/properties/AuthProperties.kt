package org.taktik.icure.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

interface AuthProperties {
	val jwt: Jwt
	val validationSkewSeconds: Long

	interface Jwt {
		val expirationSeconds: Long
		val refreshExpirationSeconds: Long
	}
}
