package org.taktik.icure.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.taktik.icure.exceptions.InvalidJwtException
import java.security.KeyPair
import java.security.PublicKey

@Component
class JwtUtils(
	@Value("\${icure.auth.jwt.expirationMillis}") val defaultExpirationTimeMillis: Long,
	@Value("\${icure.auth.jwt.refreshExpirationMillis}") private val refreshExpirationTimeMillis: Long,
) {

	val authKeyPair: KeyPair
	private val refreshKeyPair: KeyPair
	private val log = LoggerFactory.getLogger(this.javaClass)

	private val jwtEncoder: JwtEncoder<Jwt>
	private val refreshJwtEncoder: JwtEncoder<JwtRefreshDetails>

	init {
		if (System.getenv("JWT_AUTH_PUB_KEY") == null
			|| System.getenv("JWT_AUTH_PRIV_KEY") == null
			|| System.getenv("JWT_REFRESH_PUB_KEY") == null
			|| System.getenv("JWT_REFRESH_PRIV_KEY") == null) {
			authKeyPair = Keys.keyPairFor(SignatureAlgorithm.RS256)
			refreshKeyPair = Keys.keyPairFor(SignatureAlgorithm.RS256)
			log.warn("Keys for signing the JWT were auto-generated. This will not work in a clustered environment.")
		}
		else {
			authKeyPair = JwtKeyUtils.createKeyPairFromString(
				System.getenv("JWT_AUTH_PUB_KEY"),
				System.getenv("JWT_AUTH_PRIV_KEY")
			)
			refreshKeyPair = JwtKeyUtils.createKeyPairFromString(
				System.getenv("JWT_REFRESH_PUB_KEY"),
				System.getenv("JWT_REFRESH_PRIV_KEY")
			)
		}

		jwtEncoder = JwtEncoder(authKeyPair.private, defaultExpirationTimeMillis)
		refreshJwtEncoder = JwtEncoder(refreshKeyPair.private, refreshExpirationTimeMillis)
	}

	/**
	 * Creates a new JWT setting as claims the [JwtDetails] passed as parameter.
	 * The validity duration of the JWT and the key used to sign are the one specified in the configuration.
	 * @param details an instance of [JwtDetails] that contains the details to put in the token claims.
	 * @param duration the token duration of the token, in milliseconds.
	 * @return the base64-encoded JWT
	 */
	fun <T : Jwt> createJWT(details: T, duration: Long? = null): String = jwtEncoder.createJWT(details, duration)

	/**
	 * Converts the [Claims] extracted from an authentication JWT to [JwtDetails]. T
	 * @return an instance of [JwtDetails].
	 */
	fun <T : Jwt> jwtDetailsFromClaims(
		converter: JwtConverter<T>,
		it: Claims
	): T = JwtDecoder.jwtDetailsFromClaims(converter, it, defaultExpirationTimeMillis)

	/**
	 * Decodes an authentication JWT and gets the [Claims]. Throws an exception if the token is not valid or expired, unless the
	 * ignoreExpired parameter is set to true. In this case, the claims of the expired token will be used, but are not
	 * to be trusted.
	 * @param jwt the encoded JWT.
	 * @param ignoreExpiration whether to return the Claims even if the token is expired.
	 * @return the claims.
	 */
	fun <T : Jwt> decodeAndGetDetails(
		converter: JwtConverter<T>,
		jwt: String,
		ignoreExpiration: Boolean = false
	): T = jwtDetailsFromClaims(converter, decodeAndGetClaims(jwt, ignoreExpiration))

	fun decodeAndGetClaims(jwt: String, ignoreExpiration: Boolean = false, publicKey: PublicKey = authKeyPair.public): Claims =
		JwtDecoder.decodeAndGetClaims(jwt, ignoreExpiration, publicKey)

	/**
	 * Extracts the duration of the authentication JWT token from the refresh claims.
	 * If the duration was not set, the default one is returned.
	 *
	 * @param refreshJwt the base-64 encoded Refresh JWT
	 * @return the Authentication JWT duration.
	 */
	fun getJwtDurationFromRefreshToken(refreshJwt: String): Long =
		JwtDecoder.decodeAndGetClaims(refreshJwt, false, refreshKeyPair.public).let {
			(it[JWT_DURATION] as? Int?)?.toLong() ?: defaultExpirationTimeMillis
		}

	/**
	 * Creates a refresh JWT using the userId, groupID, and tokenId from the [JwtDetails] passed as parameters.
	 * @param details an instance of [JwtDetails] that contains the details to put in the token claims.
	 * @param expiration the token expiration timestamp.
	 * @return the base64-encoded refresh JWT.
	 */
	fun <T : JwtRefreshDetails> createRefreshJWT(details: T, expiration: Long? = null): String =
		refreshJwtEncoder.createJWT(
			details,
			expiration?.let { it - System.currentTimeMillis() } ?: refreshExpirationTimeMillis
		)

	/**
	 * Decodes a refresh JWT and gets the [Claims]. Throws an exception if the token is not valid or expired.
	 * @param jwt the encoded JWT.
	 * @return the [Claims].
	 */
	fun decodeAndGetRefreshClaims(jwt: String): Claims =
		JwtDecoder.decodeAndGetClaims(jwt, false, refreshKeyPair.public)

	/**
	 * Check if the token passed as parameter is not about to expire. This means that the method will check if the token
	 * will expire in 1s, avoid common cases where the method signals the token as valid when just few milliseconds of
	 * the duration are left.
	 * @param jwt the encoded JWT to verify
	 */
	fun isNotExpired(jwt: String): Boolean = JwtDecoder.isNotExpired(jwt, authKeyPair.public)

	/**
	 * @param jwt an encoded authentication JWT
	 * @return the expiration timestamp of the token.
	 */
	fun getExpirationTimestamp(jwt: String): Long = JwtDecoder.getExpirationTimestamp(jwt, authKeyPair.public)

	/**
	 * @param jwt an encoded refresh JWT
	 * @return the expiration timestamp of the token.
	 */
	fun getRefreshExpirationTimestamp(jwt: String): Long = JwtDecoder.getExpirationTimestamp(jwt, refreshKeyPair.public)

	/**
	 * @param request a [ServerHttpRequest] that contains a refresh JWT token in a `Refresh-Token` header.
	 * @return the refresh token.
	 * @throws [InvalidJwtException] if the token is missing or invalid.
	 */
	fun extractRawRefreshTokenFromRequest(request: ServerHttpRequest) = request.headers["Refresh-Token"]
		?.filterNotNull()
		?.first()
		?.replace("Bearer ", "") ?: throw InvalidJwtException("Invalid refresh token")

	/**
	 * Decodes a JWT Refresh token to a generic instance of claims [T], using the converter passed as parameter.
	 *
	 * @param converter a [JwtConverter] of [T]
	 * @param encodedToken the refresh token, encoded as a bse64 string.
	 * @return the [JwtRefreshDetails] extracted from the token.
	 */
	fun <T : JwtRefreshDetails> decodeRefreshToken(converter: JwtConverter<T>, encodedToken: String): T =
		converter.fromClaims(decodeAndGetRefreshClaims(encodedToken), defaultExpirationTimeMillis)

}
