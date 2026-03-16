package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object representing authentication configuration for a remote CouchDB instance.
 */
data class RemoteAuthenticationDto(
	/** The basic authentication credentials, if applicable. */
	val basic: BasicDto? = null,
)
