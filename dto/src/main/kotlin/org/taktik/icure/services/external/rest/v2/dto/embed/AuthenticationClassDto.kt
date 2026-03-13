package org.taktik.icure.services.external.rest.v2.dto.embed

/**
 * Enumerates the different classes of authentication that can be used to establish a user session,
 * ordered from strongest to weakest authentication assurance.
 */
enum class AuthenticationClassDto {
	DIGITAL_ID,
	TWO_FACTOR_AUTHENTICATION,
	SHORT_LIVED_TOKEN,
	EXTERNAL_AUTHENTICATION,
	PASSWORD,
	LONG_LIVED_TOKEN,
}
