package org.taktik.icure.services.external.rest.v2.dto.requests.role

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.requests.role.CreateRoleRequestDto")
data class CreateRoleRequestDto(
	@ActiveField val permissions: Set<String>,
	@ActiveField val description: String? = null,
)