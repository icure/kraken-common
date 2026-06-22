package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object representing authentication configuration for a remote CouchDB instance.
 */
data class RemoteAuthenticationDto(
	/** The basic authentication credentials, if applicable. */
	@ActiveField val basic: BasicDto? = null,
)
