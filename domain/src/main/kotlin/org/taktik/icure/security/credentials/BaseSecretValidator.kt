package org.taktik.icure.security.credentials

import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.taktik.icure.properties.AuthenticationProperties

@Component
@Profile("app")
class BaseSecretValidator(
	private val passwordEncoder: PasswordEncoder,
	private val authenticationProperties: AuthenticationProperties,
) : SecretValidator {
	override fun encodeAndValidateSecretsIfNotHashed(
		secretOrHash: String,
		secretType: SecretType,
	): String = if (!secretOrHash.matches(SecretValidator.hashedPasswordRegex)) {
		encodeAndValidateSecret(secret = secretOrHash, secretType = secretType)
	} else {
		secretOrHash
	}

	override fun encodeAndValidateSecret(
		secret: String,
		secretType: SecretType,
	): String {
		if (secretType == SecretType.PASSWORD && secret.length < authenticationProperties.recommendedPasswordLength) {
			throw IllegalArgumentException("Your password is too short. It should be at least ${authenticationProperties.recommendedPasswordLength} characters long.")
		}
		return passwordEncoder.encode(secret)
	}
}
