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
	/** The source database URL or identifier. */
	val source: String? = null,
	/** The target database URL or identifier. */
	val target: String? = null,
	/** A filter expression to apply during synchronization. */
	val filter: String? = null,
	/** The local target type for the synchronization (base, healthdata, or patient). */
	val localTarget: Target? = null,
) : Serializable {
	enum class Target {
		base,
		healthdata,
		patient,
	}
}
