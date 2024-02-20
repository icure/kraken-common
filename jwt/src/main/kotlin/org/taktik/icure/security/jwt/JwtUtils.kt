package org.taktik.icure.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultClaims
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.taktik.icure.exceptions.InvalidJwtException
import java.lang.Exception
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

@Component
class JwtUtils(
    @Value("\${icure.auth.jwt.expirationMillis}") val defaultExpirationTimeMillis: Long,
    @Value("\${icure.auth.jwt.refreshExpirationMillis}") private val refreshExpirationTimeMillis: Long,
) {

    companion object {

        private const val PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----"
        private const val PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----"
        private const val PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----"
        private const val PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----"

        fun createKeyPairFromString(publicKey: String, privateKey: String): KeyPair {
            return KeyPair(
                decodePublicKeyFromString(publicKey),
                decodePrivateKeyFromString(privateKey)
            )
        }

        private fun decodePublicKeyFromString(publicKey: String): PublicKey {
            val publicBytes = Base64.getDecoder().decode(
                publicKey
                    .replace("\n", "")
                    .replace(PUBLIC_KEY_HEADER, "")
                    .replace(PUBLIC_KEY_FOOTER, "")
            )
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePublic(X509EncodedKeySpec(publicBytes))
        }

        private fun decodePrivateKeyFromString(privateKey: String): PrivateKey {
            val privateBytes = Base64.getDecoder().decode(
                privateKey
                    .replace("\n", "")
                    .replace(PRIVATE_KEY_HEADER, "")
                    .replace(PRIVATE_KEY_FOOTER, "")
            )
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes))
        }

    }

    val authKeyPair: KeyPair
    private val refreshKeyPair: KeyPair
    private val log = LoggerFactory.getLogger(this.javaClass)

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
            authKeyPair = createKeyPairFromString(
                System.getenv("JWT_AUTH_PUB_KEY"),
                System.getenv("JWT_AUTH_PRIV_KEY")
            )
            refreshKeyPair = createKeyPairFromString(
                System.getenv("JWT_REFRESH_PUB_KEY"),
                System.getenv("JWT_REFRESH_PRIV_KEY")
            )
        }
    }

    /**
     * Creates a new JWT setting as claims the [JwtDetails] passed as parameter.
     * The validity duration of the JWT and the key used to sign are the one specified in the configuration.
     * @param details an instance of [JwtDetails] that contains the details to put in the token claims.
     * @param duration the token duration of the token, in milliseconds.
     * @return the base64-encoded JWT
     */
    fun <T : Jwt>  createJWT(details: T, duration: Long? = null): String {
        if(duration != null && duration > defaultExpirationTimeMillis)
            throw JwtException("The token duration cannot exceed the one defined by the system")
        return Jwts.builder()
            .setClaims(details.toClaims())
            .setExpiration(Date(System.currentTimeMillis() + (duration ?: defaultExpirationTimeMillis)))
            .signWith(authKeyPair.private, SignatureAlgorithm.RS256)
            .compact()
    }

    /**
     * Converts the [Claims] extracted from an authentication JWT to [JwtDetails]. T
     * @return an instance of [JwtDetails].
     */
    fun <T : Jwt> jwtDetailsFromClaims(
        converter: JwtConverter<T>,
        it: Claims
    ): T = try {
        converter.fromClaims(it, defaultExpirationTimeMillis)
    } catch (e: Exception) {
        throw InvalidJwtException("An error occurred while decoding the JWT: ${e.message}")
    }

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
    ): T =
        jwtDetailsFromClaims(converter, decodeAndGetClaims(jwt, ignoreExpiration))

    fun decodeAndGetClaims(jwt: String, ignoreExpiration: Boolean = false, publicKey: PublicKey = authKeyPair.public): Claims =
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

    /**
     * Extracts the duration of the authentication JWT token from the refresh claims.
     * If the duration was not set, the default one is returned.
     *
     * @param refreshJwt the base-64 encoded Refresh JWT
     * @return the Authentication JWT duration.
     */
    fun getJwtDurationFromRefreshToken(refreshJwt: String): Long =
        Jwts.parserBuilder()
            .setSigningKey(refreshKeyPair.public)
            .build()
            .parseClaimsJws(refreshJwt)
            .body.let {
                (it[JWT_DURATION] as? Int?)?.toLong() ?: defaultExpirationTimeMillis
            }

    /**
     * Creates a refresh JWT using the userId, groupID, and tokenId from the [JwtDetails] passed as parameters.
     * @param details an instance of [JwtDetails] that contains the details to put in the token claims.
     * @param expiration the token expiration timestamp.
     * @return the base64-encoded refresh JWT.
     */
    fun <T : JwtRefreshDetails> createRefreshJWT(details: T, expiration: Long? = null): String {
        if(expiration != null && expiration > (System.currentTimeMillis() + refreshExpirationTimeMillis))
            throw JwtException("The token duration cannot exceed the one defined by the system")
        return Jwts.builder()
            .setClaims(details.toClaims())
            .setExpiration(Date(expiration ?: (System.currentTimeMillis() + refreshExpirationTimeMillis)))
            .signWith(refreshKeyPair.private, SignatureAlgorithm.RS256)
            .compact()
    }

    /**
     * Decodes a refresh JWT and gets the [Claims]. Throws an exception if the token is not valid or expired.
     * @param jwt the encoded JWT.
     * @return the [Claims].
     */
    fun decodeAndGetRefreshClaims(jwt: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(refreshKeyPair.public)
            .build()
            .parseClaimsJws(jwt)
            .body
    }

    /**
     * Check if the token passed as parameter is not about to expire. This means that the method will check if the token
     * will expire in 1s, avoid common cases where the method signals the token as valid when just few milliseconds of
     * the duration are left.
     * @param jwt the encoded JWT to verify
     */
    fun isNotExpired(jwt: String): Boolean =
        try {
            Jwts.parserBuilder()
                .setSigningKey(authKeyPair.public)
                .build()
                .parse(jwt)
                .let { it.body as DefaultClaims }
                .expiration > Date(System.currentTimeMillis() + 1_000)
        } catch (_: ExpiredJwtException) { false }

    /**
     * @param jwt an encoded authentication JWT
     * @return the expiration timestamp of the token.
     */
    fun getExpirationTimestamp(jwt: String): Long =
        try {
            Jwts.parserBuilder()
                .setSigningKey(authKeyPair.public)
                .build()
                .parse(jwt)
                .let { it.body as DefaultClaims }
                .expiration.time
        } catch (e: ExpiredJwtException) {
            e.claims.expiration.time
        }

    /**
     * @param jwt an encoded refresh JWT
     * @return the expiration timestamp of the token.
     */
    fun getRefreshExpirationTimestamp(jwt: String): Long =
        try {
            Jwts.parserBuilder()
                .setSigningKey(refreshKeyPair.public)
                .build()
                .parse(jwt)
                .let { it.body as DefaultClaims }
                .expiration.time
        } catch (e: ExpiredJwtException) {
            e.claims.expiration.time
        }

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
