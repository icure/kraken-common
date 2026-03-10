package org.taktik.icure.services.external.rest.v2.dto.requests.topic

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.TopicRoleDto

/**
 * Request payload to add a new participant to a topic with a specified role.
 */
data class AddParticipantDto(
	/** The identifier of the data owner (user or healthcare party) to add as a participant. */
	@param:Schema(required = true)
	val dataOwnerId: String,
	/** The role to assign to the new participant within the topic. */
	@param:Schema(required = true)
	val topicRole: TopicRoleDto,
)
