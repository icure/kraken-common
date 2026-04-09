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
	/** Encrypted token */
	@param:Schema(description = "Encrypted token") val token: String? = null,
	/** Validity starting time of the token */
	@param:Schema(description = "Validity starting time of the token") val creationTime: Long = Instant.now().toEpochMilli(),
	/** Token validity in seconds. If no validity is passed, then the token never expires. (Retro compatibility for applicationTokens) */
	@param:Schema(description = "Token validity in seconds", required = true) val validity: Long,
	/** hard delete (unix epoch in ms) timestamp of the object. The deletion date will actually never be saved in the database because the corresponding tokens will be deleted from the user. */
	@param:Schema(description = "hard delete (unix epoch in ms) timestamp of the object") val deletionDate: Long? = null,
) : Serializable
