package org.taktik.icure.properties

interface AuthProperties {
	val jwt: Jwt
	val validationSkewSeconds: Long

	interface Jwt {
		val expirationSeconds: Long
		val refreshExpirationSeconds: Long
	}
}
