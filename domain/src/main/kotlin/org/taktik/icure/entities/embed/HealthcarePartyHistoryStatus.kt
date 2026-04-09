package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthcarePartyHistoryStatus(
	/** The healthcare party status at this point in time. */
	val status: HealthcarePartyStatus,
	/** The code of the specialisation this status applies to. */
	val specialisationCode: String,
	/** The start date (unix epoch in ms) of this status period. */
	val startDate: Long,
	/** Whether this status record is currently active. */
	val active: Boolean
)
