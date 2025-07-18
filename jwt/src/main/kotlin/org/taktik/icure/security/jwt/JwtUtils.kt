package org.taktik.icure.security.jwt

import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.taktik.icure.exceptions.InvalidJwtException
import org.taktik.icure.properties.AuthProperties
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Component
class JwtUtils(
	val properties: AuthProperties,
) {
	val authKeyPair: Pair<RSAPublicKey, RSAPrivateKey>
	val authPublicKeySpki: String
	val refreshKeyPair: Pair<RSAPublicKey, RSAPrivateKey>
	private val log = LoggerFactory.getLogger(this.javaClass)

	private val authJwtEncoder: JwtEncoder<Jwt>
	private val refreshJwtEncoder: JwtEncoder<JwtRefreshDetails>

	init {
		if (
			System.getenv("JWT_AUTH_PUB_KEY") == null ||
			System.getenv("JWT_AUTH_PRIV_KEY") == null ||
			System.getenv("JWT_REFRESH_PUB_KEY") == null ||
			System.getenv("JWT_REFRESH_PRIV_KEY") == null
		) {
			authKeyPair = JwtKeyUtils.generateKeyPair()
			refreshKeyPair = JwtKeyUtils.generateKeyPair()
			log.warn("Keys for signing the JWT were auto-generated. This will not work in a clustered environment.")
		} else {
			authKeyPair =
				JwtKeyUtils.decodeKeyPairFromString(
					System.getenv("JWT_AUTH_PUB_KEY"),
					System.getenv("JWT_AUTH_PRIV_KEY"),
				)
			refreshKeyPair =
				JwtKeyUtils.decodeKeyPairFromString(
					System.getenv("JWT_REFRESH_PUB_KEY"),
					System.getenv("JWT_REFRESH_PRIV_KEY"),
				)
		}

		authJwtEncoder = JwtEncoder(authKeyPair)
		refreshJwtEncoder = JwtEncoder(refreshKeyPair)
		authPublicKeySpki = JwtKeyUtils.exportSpkiBase64(authKeyPair.first)
	}

	/**
	 * Creates a new JWT setting as claims the [JwtDetails] passed as parameter.
	 * The validity duration of the JWT and the key used to sign are the one specified in the configuration.
	 * @param details an instance of [JwtDetails] that contains the details to put in the token claims.
	 * @param durationSeconds the token duration of the token, in seconds.
	 * @return the base64-encoded JWT
	 */
	fun <T : Jwt> createAuthJWT(
		details: T,
		durationSeconds: Long? = null,
	): String {
		require(durationSeconds == null || durationSeconds <= properties.jwt.expirationSeconds) {
			"The duration of a auth jwt can't be greater than ${properties.jwt.expirationSeconds} seconds (requested: $durationSeconds seconds)"
		}
		require(durationSeconds == null || durationSeconds > 0) {
			"The duration of auth jwt must be strictly positive"
		}
		return authJwtEncoder.createJWT(
			details,
			System.currentTimeMillis() + (durationSeconds ?: properties.jwt.expirationSeconds) * 1000,
		)
	}

	/**
	 * Decodes an authentication JWT and gets the claims. Throws an exception if the token is not valid or expired, unless the
	 * ignoreExpired parameter is set to true. In this case, the claims of the expired token will be used, but are not
	 * to be trusted.
	 * @param jwt the encoded JWT.
	 * @return the claims.
	 */
	suspend fun <T : Jwt> validateAndDecodeAuthDetails(
		converter: JwtConverter<T>,
		jwt: String,
	): T = converter.fromClaims(
		JwtDecoder.validateAndGetClaims(jwt, authKeyPair.first, properties.validationSkewSeconds, "com.icure.AuthJwt"),
	)

	/**
	 * Creates a refresh JWT using the userId, groupID, and tokenId from the [JwtDetails] passed as parameters.
	 * @param details an instance of [JwtDetails] that contains the details to put in the token claims.
	 * @param expirationSeconds the token expiration timestamp in seconds.
	 * @return the base64-encoded refresh JWT.
	 */
	fun createRefreshJWT(
		details: JwtRefreshDetails,
		expirationSeconds: Long? = null,
	): String = refreshJwtEncoder.createJWT(
		details,
		expirationSeconds?.let { it * 1000 }
			?: (System.currentTimeMillis() + (properties.jwt.refreshExpirationSeconds * 1000)),
	)

	/**
	 * Decodes a JWT Refresh token to a generic instance of claims [T], using the converter passed as parameter.
	 *
	 * @param converter a [JwtConverter] of [T]
	 * @param refreshJwt the refresh token, encoded as a bse64 string.
	 * @return the [JwtRefreshDetails] extracted from the token.
	 */
	suspend fun <T : JwtRefreshDetails> validateAndDecodeRefreshToken(
		converter: JwtConverter<T>,
		refreshJwt: String,
	): T = converter.fromClaims(
		JwtDecoder.validateAndGetClaims(refreshJwt, refreshKeyPair.first, properties.validationSkewSeconds, "com.icure.RefreshJwt"),
	)

	/**
	 * @param request a [ServerHttpRequest] that contains a refresh JWT token in a `Refresh-Token` header.
	 * @return the refresh token.
	 * @throws [InvalidJwtException] if the token is missing or invalid.
	 */
	fun extractRawRefreshTokenFromRequest(request: ServerHttpRequest) = request.headers["Refresh-Token"]
		?.filterNotNull()
		?.first()
		?.replace("Bearer ", "") ?: throw InvalidJwtException("Invalid refresh token")
}
