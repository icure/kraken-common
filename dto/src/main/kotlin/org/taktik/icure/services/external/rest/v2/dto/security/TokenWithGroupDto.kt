package org.taktik.icure.services.external.rest.v2.dto.security

import io.swagger.v3.oas.annotations.media.Schema

data class TokenWithGroupDto(
	@get:Schema(required = true) val token: String,
	@get:Schema(required = true) val groupId: String,
	val groupName: String? = null,
)
