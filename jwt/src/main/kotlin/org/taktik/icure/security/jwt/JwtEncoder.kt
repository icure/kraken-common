package org.taktik.icure.security.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Date

class JwtEncoder<T : Jwt>(
	keys: Pair<RSAPublicKey, RSAPrivateKey>,
) {
	private val signer =
		RSASSASigner(
			RSAKey
				.Builder(keys.first)
				.privateKey(keys.second)
				.build(),
		)

	fun createJWT(
		details: T,
		expirationTimestampMillis: Long,
	): String {
		// Create signed JWT
		val signedJWT =
			SignedJWT(
				JWSHeader.Builder(JWSAlgorithm.RS256).build(),
				JWTClaimsSet
					.Builder()
					.apply {
						details.toClaimsOmittingExpiration().forEach { (k, v) ->
							if (v != null) claim(k, v)
						}
						expirationTime(Date(expirationTimestampMillis))
					}.build(),
			)
		signedJWT.sign(signer)
		return signedJWT.serialize()
	}
}
