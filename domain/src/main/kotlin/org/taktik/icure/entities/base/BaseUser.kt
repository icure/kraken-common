package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.icure.entities.security.AuthenticationToken

interface BaseUser {
	/**
	 * Hashed version of the password (BCrypt is used for hashing).
	 */
	val passwordHash: String?

	/**
	 * Secret token used to verify 2fa
	 */
	val secret: String?

	/**
	 * Whether the user has activated two factors authentication
	 */
	val use2fa: Boolean?

	/**
	 * Deprecated : Use authenticationTokens instead - Long lived authentication tokens used for inter-applications authentication
	 */
	val applicationTokens: Map<String, String>?

	/**
	 * Encrypted and time-limited Authentication tokens used for inter-applications authentication
	 */
	val authenticationTokens: Map<String, AuthenticationToken>
}
