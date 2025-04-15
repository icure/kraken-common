package org.taktik.icure.services.external.rest.v1.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class IdentifierSelectorDto(
	val assigner: String,
	val valueField: String
)