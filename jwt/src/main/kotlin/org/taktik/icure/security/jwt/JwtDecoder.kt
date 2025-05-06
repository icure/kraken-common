package org.taktik.icure.security.jwt

import com.nimbusds.jwt.JWTParser
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.taktik.icure.exceptions.InvalidJwtException
import java.security.interfaces.RSAPublicKey

object JwtDecoder {
	private val oidcJwtDecoderCache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
		.maximumSize(10_000)
		.build<String, ReactiveJwtDecoder>()
	private var publicKeyJwtDecoderCache = mapOf<String, ReactiveJwtDecoder>()
	private val publicKeyCacheMutex = Mutex()

	private suspend fun getDecoder(publicKey: RSAPublicKey, cache: String?) =
		if (cache != null) {
			publicKeyJwtDecoderCache[cache] ?: publicKeyCacheMutex.withLock {
				publicKeyJwtDecoderCache[cache] ?: NimbusReactiveJwtDecoder.withPublicKey(publicKey).build().also {
					publicKeyJwtDecoderCache += Pair(cache, it)
				}
			}
		} else {
			NimbusReactiveJwtDecoder.withPublicKey(publicKey).build()
		}

	/**
	 * Validate a jwt using a public key and making sure it is not expired, then returns the claims.
	 * @param jwt the jwt to decode
	 * @param publicKey the public key to use to validate
	 * @param cache an optional cache identifier for the jwt decoder, if not null allows reusing the decoder. Note that
	 * the cache has no size limit and no expiration.
	 */
	suspend fun validateAndGetClaims(jwt: String, publicKey: RSAPublicKey, cache: String? = null): Map<String, Any?> = try {
		getDecoder(publicKey, cache).decode(jwt).awaitSingle().claims
	} catch (e: JwtException) {
		throw InvalidJwtException("Jwt did not pass validation", e)
	}

	suspend fun isValid(jwt: String, publicKey: RSAPublicKey, cache: String? = null): Boolean = try {
		getDecoder(publicKey, cache).decode(jwt).awaitSingle()
		true
	} catch (e: JwtException) {
		false
	}

	private fun getDecoderFromOidcIssuer(oidcIssuerLocation: String) =
		oidcJwtDecoderCache.get(oidcIssuerLocation) {
//			ReactiveJwtDecoders.fromOidcIssuerLocation(it)
			NimbusReactiveJwtDecoder.withIssuerLocation(oidcIssuerLocation).build()
		}!!

	/**
	 * Validate a jwt using an oidc issuer making sure it is not expired, then returns the claims.
	 * The decoders are cached by issuer location.
	 * @param jwt the jwt to decode
	 * @param oidcIssuerLocation the public key to use to validate
	 */
	suspend fun validateAndGetClaimsFromOidcIssuer(jwt: String, oidcIssuerLocation: String): Map<String, Any?> = try {
		getDecoderFromOidcIssuer(oidcIssuerLocation).decode(jwt).awaitSingle().claims
	} catch (e: JwtException) {
		throw InvalidJwtException("Jwt did not pass validation", e)
	}

	suspend fun isValidForOidcIssuer(jwt: String, oidcIssuerLocation: String): Boolean = try {
		getDecoderFromOidcIssuer(oidcIssuerLocation).decode(jwt).awaitSingle()
		true
	} catch (e: JwtException) {
		false
	}

	fun decodeWithoutValidation(jwt: String): Map<String, Any?> =
		JWTParser.parse(jwt).jwtClaimsSet.claims

	fun <T : Jwt> jwtDetailsFromClaims(
		converter: JwtConverter<T>,
		it: Map<String, Any?>
	): T = try {
		converter.fromClaims(it)
	} catch (e: Exception) {
		throw InvalidJwtException("An error occurred while decoding the JWT: ${e.message}")
	}
}