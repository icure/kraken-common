package org.taktik.icure.services.external.rest.v2.dto.requests.topic

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Request payload to remove an existing participant from a topic.
 */
data class RemoveParticipantDto(
	/** The identifier of the data owner to remove from the topic. */
	@param:Schema(required = true)
	val dataOwnerId: String,
)
