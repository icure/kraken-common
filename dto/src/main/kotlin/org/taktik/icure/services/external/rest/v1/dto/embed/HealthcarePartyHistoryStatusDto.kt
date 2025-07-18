package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthcarePartyHistoryStatusDto(
	val status: HealthcarePartyStatusDto,
	val specialisationCode: String,
	val startDate: Long,
	val active: Boolean,
)
