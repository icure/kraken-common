package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a historical status record for a healthcare party, tracking changes in status over time for a given specialisation.
 */
data class HealthcarePartyHistoryStatusDto(
	/** The healthcare party status at this point in time. */
	@param:Schema(required = true)
	val status: HealthcarePartyStatusDto,
	/** The code of the specialisation this status applies to. */
	@param:Schema(required = true)
	val specialisationCode: String,
	/** The start date (unix epoch in ms) of this status period. */
	@param:Schema(required = true)
	val startDate: Long,
	/** Whether this status record is currently active. */
	@param:Schema(required = true)
	val active: Boolean,
)
