package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a historical status record for a healthcare party, tracking changes in status over time for a given specialisation.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.HealthcarePartyHistoryStatusDto")
data class HealthcarePartyHistoryStatusDto(
	/** The healthcare party status at this point in time. */
	@param:Schema(required = true)
	@ActiveField val status: HealthcarePartyStatusDto,
	/** The code of the specialisation this status applies to. */
	@param:Schema(required = true)
	@ActiveField val specialisationCode: String,
	/** The start date (unix epoch in ms) of this status period. */
	@param:Schema(required = true)
	@ActiveField val startDate: Long,
	/** Whether this status record is currently active. */
	@param:Schema(required = true)
	@ActiveField val active: Boolean,
)
