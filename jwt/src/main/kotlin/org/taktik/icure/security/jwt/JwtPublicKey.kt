package org.taktik.icure.security.jwt

import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

data class JwtPublicKey(
    val n: String,
    val e: String
) {
    val publicKey: PublicKey
        get() {
            return KeyFactory.getInstance("RSA")
                .generatePublic(RSAPublicKeySpec(
                    BigInteger(1, Base64.getUrlDecoder().decode(n)),
                    BigInteger(1, Base64.getUrlDecoder().decode(e))
                ))
        }
}
