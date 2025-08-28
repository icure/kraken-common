package org.taktik.icure.services.external.rest.v2.dto.requests.topic

import io.swagger.v3.oas.annotations.media.Schema

data class RemoveParticipantDto(
	@param:Schema(required = true)
	val dataOwnerId: String,
)
