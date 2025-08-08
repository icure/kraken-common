package org.taktik.icure.security.jwt

const val JWT_DURATION = "jwtD"

interface JwtRefreshDetails : Jwt {
	val jwtDuration: Long
}
