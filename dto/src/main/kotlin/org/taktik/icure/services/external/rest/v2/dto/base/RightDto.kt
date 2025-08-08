package org.taktik.icure.services.external.rest.v2.dto.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RightDto(
	val names: Set<String> = emptySet(),
	val roles: Set<String> = emptySet(),
)
