package org.taktik.icure.services.external.rest.v2.dto.security

import java.io.Serializable
import java.time.Instant
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class OperationTokenDto(
    @Schema(description = "The hash of the token", required = true) val tokenHash: String,
    @Schema(description = "Validity starting time of the token") val creationTime: Long = Instant.now().toEpochMilli(),
    @Schema(description = "Token validity in seconds", required = true) val validity: Long,
    @Schema(description = "The operation this token is intended for", required = true) val operation: OperationDto,
    @Schema(description = "A description for the token") val description: String? = null
)
