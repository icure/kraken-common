/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.base

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SecurityDto(
	val admins: RightDto = RightDto(),
	val members: RightDto = RightDto()
) : Serializable {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
		data class RightDto(val names: Set<String> = emptySet(), val roles: Set<String> = emptySet())
}
