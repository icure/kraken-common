/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm
import org.springframework.security.web.firewall.StrictHttpFirewall
import org.taktik.icure.security.database.ShaAndVerificationCodePasswordEncoder

@Configuration
class SharedSecurityConfig {

	@Bean
	fun httpFirewall() = StrictHttpFirewall().apply {
		setAllowSemicolon(true)
	}

	@Bean
	fun passwordEncoder(): PasswordEncoder {
		val shaAndVerificationCodePasswordEncoder = ShaAndVerificationCodePasswordEncoder("SHA-256")

		return object : DelegatingPasswordEncoder(
			"sha256",
			mapOf(
				"sha256" to shaAndVerificationCodePasswordEncoder,
				"bcrypt" to BCryptPasswordEncoder(12),
				"pbkdf2" to Pbkdf2PasswordEncoder("", 8, 185000, SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256),
			)
		) {
			override fun encode(rawPassword: CharSequence?): String {
				// TODO later: remove this when we phase out older versions of kraken (from pre-split era)
				return super.encode(rawPassword).replaceFirst("{sha256}", "")
			}
		}.apply { setDefaultPasswordEncoderForMatches(shaAndVerificationCodePasswordEncoder) }
	}
}
