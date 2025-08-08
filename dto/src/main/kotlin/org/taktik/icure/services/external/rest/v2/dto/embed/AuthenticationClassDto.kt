package org.taktik.icure.services.external.rest.v2.dto.embed

enum class AuthenticationClassDto {
	DIGITAL_ID,
	TWO_FACTOR_AUTHENTICATION,
	SHORT_LIVED_TOKEN,
	EXTERNAL_AUTHENTICATION,
	PASSWORD,
	LONG_LIVED_TOKEN,
}
