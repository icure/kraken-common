package org.taktik.icure.security.jwt

import com.nimbusds.jwt.JWTParser
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.taktik.icure.exceptions.InvalidJwtException
import java.security.interfaces.RSAPublicKey
import java.time.Duration

object JwtDecoder {
	private val oidcJwtDecoderCache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
		.maximumSize(10_000)
		.build<Pair<String, Long>, ReactiveJwtDecoder>()
	private var publicKeyJwtDecoderCache = mapOf<Pair<String, Long>, ReactiveJwtDecoder>()
	private val publicKeyCacheMutex = Mutex()

	private fun getValidators(validationSkewSeconds: Long) =
		JwtValidators.createDefaultWithValidators(JwtTimestampValidator(Duration.ofSeconds(validationSkewSeconds)))


	private fun createPublicKeyDecoder(publicKey: RSAPublicKey, validationSkewSeconds: Long): NimbusReactiveJwtDecoder =
		NimbusReactiveJwtDecoder.withPublicKey(publicKey).build().also {
			it.setJwtValidator(getValidators(validationSkewSeconds))
		}

	private suspend fun getDecoder(publicKey: RSAPublicKey, validationSkewSeconds: Long, cache: String?) =
		if (cache != null) {
			val key = Pair(cache, validationSkewSeconds)
			publicKeyJwtDecoderCache[key] ?: publicKeyCacheMutex.withLock {
				publicKeyJwtDecoderCache[key] ?: createPublicKeyDecoder(publicKey, validationSkewSeconds).also {
					publicKeyJwtDecoderCache += Pair(key, it)
				}
			}
		} else createPublicKeyDecoder(publicKey, validationSkewSeconds)

	/**
	 * Validate a jwt using a public key and making sure it is not expired, then returns the claims.
	 * @param jwt the jwt to decode
	 * @param publicKey the public key to use to validate
	 * @param cache an optional cache identifier for the jwt decoder, if not null allows reusing the decoder. Note that
	 * the cache has no size limit and no expiration.
	 */
	suspend fun validateAndGetClaims(jwt: String, publicKey: RSAPublicKey, validationSkewSeconds: Long, cache: String? = null): Map<String, Any?> = try {
		getDecoder(publicKey, validationSkewSeconds, cache).decode(jwt).awaitSingle().claims
	} catch (e: JwtException) {
		throw InvalidJwtException("Jwt did not pass validation", e)
	}

	suspend fun isValid(jwt: String, publicKey: RSAPublicKey, validationSkewSeconds: Long, cache: String? = null): Boolean = try {
		getDecoder(publicKey, validationSkewSeconds, cache).decode(jwt).awaitSingle()
		true
	} catch (e: JwtException) {
		false
	}

	private fun getDecoderFromOidcIssuer(oidcIssuerLocation: String, validationSkewSeconds: Long) =
		oidcJwtDecoderCache.get(Pair(oidcIssuerLocation, validationSkewSeconds)) {
			NimbusReactiveJwtDecoder.withIssuerLocation(oidcIssuerLocation).build().also {
				it.setJwtValidator(getValidators(validationSkewSeconds))
			}
		}!!

	/**
	 * Validate a jwt using an oidc issuer making sure it is not expired, then returns the claims.
	 * The decoders are cached by issuer location.
	 * @param jwt the jwt to decode
	 * @param oidcIssuerLocation the public key to use to validate
	 */
	suspend fun validateAndGetClaimsFromOidcIssuer(jwt: String, oidcIssuerLocation: String, validationSkewSeconds: Long): Map<String, Any?> = try {
		getDecoderFromOidcIssuer(oidcIssuerLocation, validationSkewSeconds).decode(jwt).awaitSingle().claims
	} catch (e: JwtException) {
		throw InvalidJwtException("Jwt did not pass validation", e)
	}

	suspend fun isValidForOidcIssuer(jwt: String, oidcIssuerLocation: String, validationSkewSeconds: Long): Boolean = try {
		getDecoderFromOidcIssuer(oidcIssuerLocation, validationSkewSeconds).decode(jwt).awaitSingle()
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