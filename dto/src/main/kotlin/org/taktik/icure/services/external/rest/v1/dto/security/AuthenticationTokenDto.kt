package org.taktik.icure.services.external.rest.v1.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AuthenticationTokenDto(
	@param:Schema(description = "Encrypted token") val token: String?,
	@param:Schema(description = "Validity starting time of the token") val creationTime: Long = Instant.now().toEpochMilli(),
	@param:Schema(description = "Token validity in seconds") val validity: Long,
	@param:Schema(description = "hard delete (unix epoch in ms) timestamp of the object") val deletionDate: Long? = null,
) : Cloneable,
	Serializable
