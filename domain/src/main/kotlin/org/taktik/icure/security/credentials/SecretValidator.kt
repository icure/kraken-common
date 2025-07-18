package org.taktik.icure.security.credentials

/**
 * This interface defines a method to verify if a password or a token meets the security requirements and encode it.
 */
interface SecretValidator {

	companion object {
		val hashedPasswordRegex = Regex("^(\\{.+?})?[0-9a-zA-Z]{64}$")
	}

	/**
	 * This method receives a secret or the hash of a secret an the [SecretType] of the secret.
	 * If the secret is a hash, then it returns it directly. Otherwise, it verifies it according to criteria that
	 * are based on the [SecretType] and on the concrete implementation.
	 * If the secret does not meet the security criteria, then an [IllegalArgumentException] is thrown, otherwise the
	 * hashed secret is returned.
	 *
	 * @param secretOrHash a secret (token, password, or other) or a hashed secret.
	 * @param secretType the [SecretType] of the secret.
	 * @return the hashed secret.
	 * @throws IllegalArgumentException if the secret is not hashed and does not meet the security criteria defined by
	 * the function.
	 */
	fun encodeAndValidateSecrets(secretOrHash: String, secretType: SecretType): String
}
