package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthcarePartyHistoryStatus(
	val status: HealthcarePartyStatus,
	val specialisationCode: String,
	val startDate: Long,
	val active: Boolean
)
