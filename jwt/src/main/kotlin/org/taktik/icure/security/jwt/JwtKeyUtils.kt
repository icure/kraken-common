package org.taktik.icure.security.jwt

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64


object JwtKeyUtils {

	private const val PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----"
	private const val PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----"
	private const val PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----"
	private const val PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----"

	fun decodeKeyPairFromString(publicKey: String, privateKey: String): Pair<RSAPublicKey, RSAPrivateKey> {
		return Pair(
			decodePublicKeyFromString(publicKey),
			decodePrivateKeyFromString(privateKey)
		)
	}

	fun decodePublicKeyFromString(publicKey: String): RSAPublicKey {
		val publicBytes = Base64.getDecoder().decode(
			publicKey
				.replace("\n", "")
				.replace(PUBLIC_KEY_HEADER, "")
				.replace(PUBLIC_KEY_FOOTER, "")
		)
		val keyFactory = KeyFactory.getInstance("RSA")
		return keyFactory.generatePublic(X509EncodedKeySpec(publicBytes)) as RSAPublicKey
	}

	private fun decodePrivateKeyFromString(privateKey: String): RSAPrivateKey {
		val privateBytes = Base64.getDecoder().decode(
			privateKey
				.replace("\n", "")
				.replace(PRIVATE_KEY_HEADER, "")
				.replace(PRIVATE_KEY_FOOTER, "")
		)
		val keyFactory = KeyFactory.getInstance("RSA")
		return keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes)) as RSAPrivateKey
	}

	fun generateKeyPair(): Pair<RSAPublicKey, RSAPrivateKey> {
		val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
		keyPairGenerator.initialize(2048) // Key size
		val keyPair = keyPairGenerator.generateKeyPair()
		return Pair(
			keyPair.public as RSAPublicKey,
			keyPair.private as RSAPrivateKey
		)
	}

	fun exportSpkiBase64(publicKey: RSAPublicKey): String {
		val keyFactory = KeyFactory.getInstance("RSA")
		val x509Spec: X509EncodedKeySpec = keyFactory.getKeySpec(publicKey, X509EncodedKeySpec::class.java)
		val spkiBytes = x509Spec.encoded
		return Base64.getEncoder().encodeToString(spkiBytes)
	}
}