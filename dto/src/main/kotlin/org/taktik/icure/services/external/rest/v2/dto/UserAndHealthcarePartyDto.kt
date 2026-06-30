package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity embeds a User and its linked Healthcare Party.""")
/**
 * DTO that embeds a user and its linked healthcare party, used when both entities
 * need to be returned together.
 */
data class UserAndHealthcarePartyDto(
	/** The user entity. */
	@param:Schema(required = true) @ActiveField val user: UserDto,
	/** The healthcare party linked to the user. */
	@param:Schema(required = true) @ActiveField val healthcareParty: HealthcarePartyDto,
)
