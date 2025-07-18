/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.CodeStub
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdministrationQuantity(
	val quantity: Double? = null,
	val administrationUnit: CodeStub? = null, // CD-ADMINISTRATIONUNIT
	val unit: String? = null, // Should be null
) : Serializable {
	override fun toString(): String = String.format("%f %s", quantity, if (administrationUnit != null) administrationUnit.code else unit)
}
