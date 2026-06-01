package org.taktik.icure.services.external.rest.v2.dto.requests.role

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
data class CreateRoleRequestDto(
	val permissions: Set<String>,
	val description: String? = null,
)