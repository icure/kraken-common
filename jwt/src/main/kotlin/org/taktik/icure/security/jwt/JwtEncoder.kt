package org.taktik.icure.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.PrivateKey
import java.util.*

class JwtEncoder<T : Jwt>(
	private val privateKey: PrivateKey,
	private val expirationMillis: Long
) {

	fun createJWT(details: T, duration: Long? = null): String {
		if(duration != null && duration > expirationMillis)
			throw JwtException("The token duration cannot exceed the one defined by the system")
		return Jwts.builder()
			.setClaims(details.toClaims())
			.setExpiration(Date(System.currentTimeMillis() + (duration ?: expirationMillis)))
			.signWith(privateKey, SignatureAlgorithm.RS256)
			.compact()
	}

}