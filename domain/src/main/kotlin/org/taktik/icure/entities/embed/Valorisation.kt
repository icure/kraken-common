/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Valorisation(
	@param:ContentValue(ContentValues.FUZZY_DATE) val startOfValidity: Long? = null, //yyyyMMdd
	@param:ContentValue(ContentValues.FUZZY_DATE) val endOfValidity: Long? = null, //yyyyMMdd
	@param:ContentValue(ContentValues.ANY_STRING) val predicate: String? = null,
	val reference: List<Int>? = null,
	@param:ContentValue(ContentValues.ANY_DOUBLE) val totalAmount: Double? = null, //=reimbursement+doctorSupplement+intervention
	@param:ContentValue(ContentValues.ANY_DOUBLE) val reimbursement: Double? = null,
	@param:ContentValue(ContentValues.ANY_DOUBLE) val patientIntervention: Double? = null,
	@param:ContentValue(ContentValues.ANY_DOUBLE) val doctorSupplement: Double? = null,
	@param:ContentValue(ContentValues.ANY_DOUBLE) val vat: Double? = null,
	val label: Map<String, String>? = null, //ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}
	override val encryptedSelf: String? = null
) : Encrypted, Serializable
