/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PostalCode(
	/** The postal code value. */
	val code: String? = null,
	/** Localized labels for this postal code, keyed by language code. */
	val label: Map<String, String> = emptyMap(),
) : Serializable {
	override fun toString(): String = code ?: "N/A"
}
