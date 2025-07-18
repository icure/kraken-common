/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseSynchronization(
	val source: String? = null,
	val target: String? = null,
	val filter: String? = null,
	val localTarget: Target? = null,
) : Serializable {
	enum class Target {
		base,
		healthdata,
		patient,
	}
}
