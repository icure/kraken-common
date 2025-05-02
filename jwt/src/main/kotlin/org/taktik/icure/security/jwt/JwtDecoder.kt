package org.taktik.icure.security.jwt

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders
import org.taktik.icure.exceptions.InvalidJwtException
import java.security.interfaces.RSAPublicKey

object JwtDecoder {
	private val oidcJwtDecoderCache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
		.maximumSize(10_000)
		.build<String, ReactiveJwtDecoder>()
	private var publicKeyJwtDecoderCache = mapOf<String, ReactiveJwtDecoder>()
	private val publicKeyCacheMutex = Mutex()

	/**
	 * Validate a jwt using a public key and making sure it is not expired, then returns the claims.
	 * @param jwt the jwt to decode
	 * @param publicKey the public key to use to validate
	 * @param cache an optional cache identifier for the jwt decoder, if not null allows reusing the decoder. Note that
	 * the cache has no size limit and no expiration.
	 */
	suspend fun validateAndGetClaims(jwt: String, publicKey: RSAPublicKey, cache: String? = null): Map<String, Any?> = try {
		val decoder = if (cache != null) {
			publicKeyJwtDecoderCache[cache] ?: publicKeyCacheMutex.withLock {
				publicKeyJwtDecoderCache[cache] ?: NimbusReactiveJwtDecoder.withPublicKey(publicKey).build().also {
					publicKeyJwtDecoderCache += Pair(cache, it)
				}
			}
		} else {
			NimbusReactiveJwtDecoder.withPublicKey(publicKey).build()
		}
		decoder.decode(jwt).awaitSingle().claims
	} catch (e: JwtException) {
		throw InvalidJwtException("Jwt did not pass validation", e)
	}

	/**
	 * Validate a jwt using an oidc issuer making sure it is not expired, then returns the claims.
	 * The decoders are cached by issuer location.
	 * @param jwt the jwt to decode
	 * @param oidcIssuerLocation the public key to use to validate
	 */
	suspend fun validateAndGetClaimsFromOidcIssuer(jwt: String, oidcIssuerLocation: String): Map<String, Any?> = try {
		val decoder = oidcJwtDecoderCache.get(oidcIssuerLocation) {
			ReactiveJwtDecoders.fromOidcIssuerLocation(it)
		}!!
		decoder.decode(jwt).awaitSingle().claims
	} catch (e: JwtException) {
		throw InvalidJwtException("Jwt did not pass validation", e)
	}

	fun <T : Jwt> jwtDetailsFromClaims(
		converter: JwtConverter<T>,
		it: Map<String, Any?>
	): T = try {
		converter.fromClaims(it)
	} catch (e: Exception) {
		throw InvalidJwtException("An error occurred while decoding the JWT: ${e.message}")
	}
}