/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
//@Mergeable(["predicate", "startOfValidity", "endOfValidity"])
data class Valorisation(
	val startOfValidity: Long? = null, // yyyyMMdd
	val endOfValidity: Long? = null, // yyyyMMdd
	val predicate: String? = null,
	val reference: List<Int>? = null,
	val totalAmount: Double? = null, // =reimbursement+doctorSupplement+intervention
	val reimbursement: Double? = null,
	val patientIntervention: Double? = null,
	val doctorSupplement: Double? = null,
	val vat: Double? = null,
	val label: Map<String, String>? = null, // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable
