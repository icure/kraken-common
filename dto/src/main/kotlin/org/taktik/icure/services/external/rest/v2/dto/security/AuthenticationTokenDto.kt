package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AuthenticationTokenDto(
	@get:Schema(description = "Encrypted token") val token: String? = null,
	@get:Schema(description = "Validity starting time of the token") val creationTime: Long = Instant.now().toEpochMilli(),
	@get:Schema(description = "Token validity in seconds", required = true) val validity: Long,
	@get:Schema(description = "hard delete (unix epoch in ms) timestamp of the object") val deletionDate: Long? = null,
) : Serializable
