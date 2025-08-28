package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthcarePartyHistoryStatusDto(
	@param:Schema(required = true)
	val status: HealthcarePartyStatusDto,
	@param:Schema(required = true)
	val specialisationCode: String,
	@param:Schema(required = true)
	val startDate: Long,
	@param:Schema(required = true)
	val active: Boolean,
)
