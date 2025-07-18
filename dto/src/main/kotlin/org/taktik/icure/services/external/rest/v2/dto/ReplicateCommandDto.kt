package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReplicateCommandDto(
	@JsonProperty("source_url") @Schema(required = true) val sourceUrl: String,
	@JsonProperty("source_username") @Schema(required = true) val sourceUsername: String,
	@JsonProperty("source_password") @Schema(required = true) val sourcePassword: String,
	@JsonProperty("target_url") @Schema(required = true) val targetUrl: String,
	@JsonProperty("target_username") @Schema(required = true) val targetUsername: String,
	@JsonProperty("target_password") @Schema(required = true) val targetPassword: String,
	val id: String? = null,
)
