package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegistrationInformationDto(
	val firstName: String,
	val lastName: String,
	val emailAddress: String,
	val userOptions: String? = null,
	@JsonInclude(JsonInclude.Include.NON_EMPTY) val userRoles:Set<String> = emptySet(),
	val minimumKrakenVersion: String? = null,
)
