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
	/** The numeric quantity to administer. */
	val quantity: Double? = null,
	/** The coded unit of administration (CD-ADMINISTRATIONUNIT). */
	val administrationUnit: CodeStub? = null,
	/** A textual representation of the unit. Should be null if administrationUnit is set/ */
	val unit: String? = null,
) : Serializable {
	override fun toString(): String = String.format("%f %s", quantity, if (administrationUnit != null) administrationUnit.code else unit)
}
