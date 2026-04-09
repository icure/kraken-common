/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.embed.TypedValuesType
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PropertyTypeStub(
	/** The human-readable identifier of this property type. */
	val identifier: String? = null,
	/** The value type of this property type. */
	val type: TypedValuesType? = null,
) : Serializable
