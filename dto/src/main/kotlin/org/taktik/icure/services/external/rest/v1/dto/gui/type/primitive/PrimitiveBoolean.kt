/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.gui.type.primitive

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.gui.type.Data
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class PrimitiveBoolean(
	val value: Boolean? = null,
) : Data(),
	Primitive {
	override fun getPrimitiveValue(): Serializable? = value

	companion object {
		val positiveValues = listOf("yes", "ja", "oui", "si", "yo", "ok")
	}
}
