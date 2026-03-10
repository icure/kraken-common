package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an authentication token associated with a user, including its creation time and validity period.
 * The token value is stored in encrypted form.
 */
data class AuthenticationTokenDto(
	/** The encrypted token string. */
	@param:Schema(description = "Encrypted token") val token: String? = null,
	/** The epoch-millisecond timestamp at which the token became valid. */
	@param:Schema(description = "Validity starting time of the token") val creationTime: Long = Instant.now().toEpochMilli(),
	/** The duration in seconds for which the token remains valid after creation. */
	@param:Schema(description = "Token validity in seconds", required = true) val validity: Long,
	/** The epoch-millisecond timestamp of a hard deletion, if the token has been marked for deletion. */
	@param:Schema(description = "hard delete (unix epoch in ms) timestamp of the object") val deletionDate: Long? = null,
) : Serializable
