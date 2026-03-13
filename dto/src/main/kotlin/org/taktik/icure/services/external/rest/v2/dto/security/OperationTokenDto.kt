package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a short-lived token that authorizes a single privileged operation (e.g. group transfer).
 * The token is stored as a hash rather than in plain text.
 */
data class OperationTokenDto(
	/** The hash of the operation token. */
	@param:Schema(description = "The hash of the token", required = true) val tokenHash: String,
	/** The epoch-millisecond timestamp at which the token was created. */
	@param:Schema(description = "Validity starting time of the token") val creationTime: Long = Instant.now().toEpochMilli(),
	/** The duration in seconds for which the token remains valid after creation. */
	@param:Schema(description = "Token validity in seconds", required = true) val validity: Long,
	/** The specific operation this token is intended to authorize. */
	@param:Schema(description = "The operation this token is intended for", required = true) val operation: OperationDto,
	/** An optional human-readable description of the token's purpose. */
	@param:Schema(description = "A description for the token") val description: String? = null,
)
