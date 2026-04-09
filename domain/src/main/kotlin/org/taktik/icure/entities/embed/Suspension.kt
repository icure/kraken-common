/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Suspension(
	/** The start moment of the suspension (fuzzy date). */
	val beginMoment: Long? = null,
	/** The end moment of the suspension (fuzzy date). */
	val endMoment: Long? = null,
	/** The reason for the suspension. */
	val suspensionReason: String? = null,
	/** The lifecycle state of the suspension. */
	val lifecycle: String? = null,
) : Serializable
