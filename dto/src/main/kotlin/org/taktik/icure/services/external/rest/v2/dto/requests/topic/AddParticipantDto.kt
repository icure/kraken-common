package org.taktik.icure.services.external.rest.v2.dto.requests.topic

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.TopicRoleDto

data class AddParticipantDto(
	@param:Schema(required = true)
	val dataOwnerId: String,
	@param:Schema(required = true)
	val topicRole: TopicRoleDto,
)
