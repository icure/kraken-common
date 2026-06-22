package org.taktik.icure.services.external.rest.v2.dto.security

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.security.ChangeUserPasswordRequestDto")
data class ChangeUserPasswordRequestDto(
	@param:Schema(required = true)
	@ActiveField val newPassword: String,
)