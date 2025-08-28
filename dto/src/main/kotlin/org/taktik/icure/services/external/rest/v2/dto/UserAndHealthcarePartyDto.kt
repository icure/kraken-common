package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity embeds a User and its linked Healthcare Party.""")
data class UserAndHealthcarePartyDto(
	@param:Schema(required = true) val user: UserDto,
	@param:Schema(required = true) val healthcareParty: HealthcarePartyDto,
)
