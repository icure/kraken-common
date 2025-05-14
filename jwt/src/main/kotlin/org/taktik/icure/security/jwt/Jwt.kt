package org.taktik.icure.security.jwt

const val USER_ID = "u"

interface Jwt {

    val userId: String
    /**
     * Expiration date of jwt in epoch seconds: note this value is ignored when creating a new jwt, and it should be
     * not null only when the JwtDetails were deserialized.
     */
    val expiration: Long?

    fun toClaimsOmittingExpiration(): Map<String, Any?>

    object StandardClaims {
        const val AUDIENCE = "aud"
        const val EXPIRES_AT = "exp"
        const val JTI = "jti"
        const val ISSUED_AT = "iat"
        const val ISSUER = "iss"
        const val NOT_BEFORE = "nbf"
        const val SUBJECT = "sub"
    }
}

interface JwtConverter<T : Jwt> {
    fun fromClaims(claims: Map<String, Any?>): T
}