package org.taktik.icure.services.external.rest.v1.dto.security

import java.io.Serializable
import java.time.Instant
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class OperationTokenDto(
    @Schema(description = "The hash of the token") val tokenHash: String,
    @Schema(description = "Validity starting time of the token") val creationTime: Long = Instant.now().toEpochMilli(),
    @Schema(description = "Token validity in seconds") val validity: Long,
    @Schema(description = "The operation this token is intended for") val operation: Operation,
    @Schema(description = "A description for the token") val description: String? = null
) : Cloneable, Serializable {

    companion object {
        enum class Operation { TRANSFER_GROUP }
    }

}
