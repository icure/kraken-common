package org.taktik.icure.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.impl.DefaultClaims
import org.taktik.icure.exceptions.InvalidJwtException
import java.security.PublicKey
import java.util.*

object JwtDecoder {

	fun decodeAndGetClaims(jwt: String, ignoreExpiration: Boolean = false, publicKey: PublicKey): Claims =
		try {
			Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(jwt)
				.body
		} catch (e: ExpiredJwtException) {
			if(ignoreExpiration) {
				e.claims
			} else throw e
		}

	fun isNotExpired(jwt: String, publicKey: PublicKey): Boolean =
		try {
			Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parse(jwt)
				.let { it.body as DefaultClaims }
				.expiration > Date(System.currentTimeMillis() + 1_000)
		} catch (_: ExpiredJwtException) { false }

	fun getExpirationTimestamp(jwt: String, publicKey: PublicKey): Long =
		try {
			Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parse(jwt)
				.let { it.body as DefaultClaims }
				.expiration.time
		} catch (e: ExpiredJwtException) {
			e.claims.expiration.time
		}

	fun <T : Jwt> jwtDetailsFromClaims(
		converter: JwtConverter<T>,
		it: Claims,
		expirationMillis: Long
	): T = try {
		converter.fromClaims(it, expirationMillis)
	} catch (e: Exception) {
		throw InvalidJwtException("An error occurred while decoding the JWT: ${e.message}")
	}
}