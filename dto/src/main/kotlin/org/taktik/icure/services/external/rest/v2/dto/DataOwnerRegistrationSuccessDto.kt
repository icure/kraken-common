package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DataOwnerRegistrationSuccessDto(
	@Schema(required = true) val userLogin: String,
	@Schema(required = true) val userId: String,
	@Schema(required = true) val token: String
)
