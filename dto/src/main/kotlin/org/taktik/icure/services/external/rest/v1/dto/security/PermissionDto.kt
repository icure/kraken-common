/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PermissionDto(
	@get:Schema(description = "Granted permissions.") val grants: Set<PermissionItemDto> = emptySet(),
	@get:Schema(description = "Revoked permissions.") val revokes: Set<PermissionItemDto> = emptySet(),
) : Cloneable,
	Serializable
