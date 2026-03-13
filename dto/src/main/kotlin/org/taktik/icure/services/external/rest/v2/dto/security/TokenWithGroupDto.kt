package org.taktik.icure.services.external.rest.v2.dto.security

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Associates a JWT or authentication token with the group context in which it is valid.
 */
data class TokenWithGroupDto(
	/** The authentication token string. */
	@param:Schema(required = true) val token: String,
	/** The identifier of the group this token is scoped to. */
	@param:Schema(required = true) val groupId: String,
	/** The human-readable name of the group, if available. */
	val groupName: String? = null,
)
