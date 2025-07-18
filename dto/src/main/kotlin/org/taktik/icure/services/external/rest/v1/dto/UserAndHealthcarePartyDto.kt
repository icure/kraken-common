package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity embeds a User and its linked Healthcare Party.""")
data class UserAndHealthcarePartyDto(
	val user: UserDto,
	val healthcareParty: HealthcarePartyDto,
)
