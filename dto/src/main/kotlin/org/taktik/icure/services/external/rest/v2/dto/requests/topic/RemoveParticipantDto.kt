package org.taktik.icure.services.external.rest.v2.dto.requests.topic

import io.swagger.v3.oas.annotations.media.Schema

data class RemoveParticipantDto(
    @Schema(required = true)
    val dataOwnerId: String,
)
