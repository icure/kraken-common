package org.taktik.icure.security.jwt

import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

object JwtKeyUtils {

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

	fun decodePublicKeyFromString(publicKey: String): PublicKey {
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