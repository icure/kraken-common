package org.taktik.icure.entities.embed

/**
 * Represents different ways a user can authenticate
 * @param level The level of the authentication class. Higher is more secure.
 */
enum class AuthenticationClass(val level: Int) {
	/**
	 * The user has authenticated using a digital ID provider (e.g. itsme, spid, ...).
	 */
	DIGITAL_ID(60),

	/**
	 * The user has authenticated with iCure's 2fa.
	 */
	TWO_FACTOR_AUTHENTICATION(50),

	/**
	 * The user has authenticated using a short-lived token
	 */
	SHORT_LIVED_TOKEN(40),

	/**
	 * The user has authenticated using an external authentication provider (e.g. Oauth/Google)
	 */
	EXTERNAL_AUTHENTICATION(30),

	/**
	 * The user has authenticated using his personal password.
	 */
	PASSWORD(20),

	/**
	 * The user has authenticated using a long-lived token. Less safe than password because the usage of long lived
	 * tokens is recommended in order to store locally a session.
	 */
	LONG_LIVED_TOKEN(10),
	;

	companion object {
		/**
		 * Header used together with a 401 error caused by a request to an operation requiring elevated security to
		 * indicate the minimum required auth level to perform that operation.
		 */
		const val MINIMUM_REQUIRED_AUTH_LEVEL_HEADER = "Icure-Minimum-Required-Auth-Level"

		fun fromLevelOrNull(level: Int) = values().firstOrNull { it.level == level }

		fun fromLevel(level: Int) = requireNotNull(fromLevelOrNull(level)) {
			"No authentication class found for level $level"
		}
	}
}
