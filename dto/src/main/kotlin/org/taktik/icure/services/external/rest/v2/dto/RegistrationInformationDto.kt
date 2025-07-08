package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegistrationInformationDto(
	val firstName: String? = null,
	val lastName: String? = null,
	val companyName: String? = null,
	@Schema(required = true) val emailAddress: String,
	val userOptions: String? = null,
	val userRoles:Set<String> = emptySet(),
	val minimumKrakenVersion: String? = null,
	val cluster: String? = null
)
