package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object representing basic authentication credentials for a CouchDB remote connection.
 */
data class BasicDto(
	/** The username for authentication. */
	@param:Schema(required = true)
	val username: String,
	/** The password for authentication. */
	@param:Schema(required = true)
	val password: String,
)
