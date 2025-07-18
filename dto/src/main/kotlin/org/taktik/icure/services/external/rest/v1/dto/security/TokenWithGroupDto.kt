package org.taktik.icure.services.external.rest.v1.dto.security

data class TokenWithGroupDto(
	val token: String,
	val groupId: String,
	val groupName: String?,
)
