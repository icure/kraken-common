/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RightDto(
	val userId: String? = null,
	@get:Schema(defaultValue = "false") val read: Boolean = false,
	@get:Schema(defaultValue = "false") val write: Boolean = false,
	@get:Schema(defaultValue = "false") val administration: Boolean = false,
) : Serializable
