package org.taktik.icure.services.external.rest.v2.dto.security

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Associates a JWT or authentication token with the group context in which it is valid.
 */
data class TokenWithGroupDto(
	/** The authentication token string. */
	@param:Schema(required = true) @ActiveField val token: String,
	/** The identifier of the group this token is scoped to. */
	@param:Schema(required = true) @ActiveField val groupId: String,
	/** The human-readable name of the group, if available. */
	@ActiveField val groupName: String? = null,
)
