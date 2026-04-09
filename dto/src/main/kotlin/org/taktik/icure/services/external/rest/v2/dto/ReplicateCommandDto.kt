package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * DTO representing a command to set up database replication between a source and target CouchDB instance.
 */
data class ReplicateCommandDto(
	/** The URL of the source CouchDB database. */
	@param:JsonProperty("source_url") @param:Schema(required = true) val sourceUrl: String,
	/** The username for authenticating with the source database. */
	@param:JsonProperty("source_username") @param:Schema(required = true) val sourceUsername: String,
	/** The password for authenticating with the source database. */
	@param:JsonProperty("source_password") @param:Schema(required = true) val sourcePassword: String,
	/** The URL of the target CouchDB database. */
	@param:JsonProperty("target_url") @param:Schema(required = true) val targetUrl: String,
	/** The username for authenticating with the target database. */
	@param:JsonProperty("target_username") @param:Schema(required = true) val targetUsername: String,
	/** The password for authenticating with the target database. */
	@param:JsonProperty("target_password") @param:Schema(required = true) val targetPassword: String,
	/** The optional identifier of this replication command. */
	val id: String? = null,
)
