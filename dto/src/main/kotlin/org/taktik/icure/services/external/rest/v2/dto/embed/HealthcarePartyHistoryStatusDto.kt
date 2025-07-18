package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthcarePartyHistoryStatusDto(
	@get:Schema(required = true)
	val status: HealthcarePartyStatusDto,
	@get:Schema(required = true)
	val specialisationCode: String,
	@get:Schema(required = true)
	val startDate: Long,
	@get:Schema(required = true)
	val active: Boolean,
)
