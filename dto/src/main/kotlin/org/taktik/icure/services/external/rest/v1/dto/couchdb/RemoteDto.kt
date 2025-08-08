package org.taktik.icure.services.external.rest.v1.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RemoteDto(
	val url: String,
	val auth: RemoteAuthenticationDto? = null,
)
