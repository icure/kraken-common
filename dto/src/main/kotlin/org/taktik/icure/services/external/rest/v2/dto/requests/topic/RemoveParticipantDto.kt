package org.taktik.icure.services.external.rest.v2.dto.requests.topic

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Request payload to remove an existing participant from a topic.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.requests.topic.RemoveParticipantDto")
data class RemoveParticipantDto(
	/** The identifier of the data owner to remove from the topic. */
	@param:Schema(required = true)
	@ActiveField val dataOwnerId: String,
)
