/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseSynchronizationDto(
	val source: String? = null,
	val target: String? = null,
	val filter: String? = null,
	val localTarget: Target? = null
) : Serializable {
	enum class Target {
		base, healthdata, patient
	}
}
