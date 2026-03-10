package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object representing a remote CouchDB endpoint with its URL and optional authentication.
 */
data class RemoteDto(
	/** The URL of the remote CouchDB instance. */
	@param:Schema(required = true)
	val url: String,
	/** The authentication configuration for connecting to the remote instance. */
	val auth: RemoteAuthenticationDto? = null,
)
