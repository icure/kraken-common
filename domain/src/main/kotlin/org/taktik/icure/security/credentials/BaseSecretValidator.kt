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
	override fun encodeAndValidateSecrets(
		secretOrHash: String,
		secretType: SecretType,
	): String = if (!secretOrHash.matches(SecretValidator.hashedPasswordRegex)) {
		if (secretType == SecretType.PASSWORD && secretOrHash.length < authenticationProperties.recommendedPasswordLength) {
			throw IllegalArgumentException("Your password is too short. It should be at least ${authenticationProperties.recommendedPasswordLength} characters long.")
		}
		passwordEncoder.encode(secretOrHash)
	} else {
		secretOrHash
	}
}
