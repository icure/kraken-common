package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class OperationTokenDto(
	@get:Schema(description = "The hash of the token", required = true) val tokenHash: String,
	@get:Schema(description = "Validity starting time of the token") val creationTime: Long = Instant.now().toEpochMilli(),
	@get:Schema(description = "Token validity in seconds", required = true) val validity: Long,
	@get:Schema(description = "The operation this token is intended for", required = true) val operation: OperationDto,
	@get:Schema(description = "A description for the token") val description: String? = null,
)
