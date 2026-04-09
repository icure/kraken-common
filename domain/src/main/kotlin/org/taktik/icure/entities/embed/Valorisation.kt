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
	/** The start of the validity period (yyyyMMdd). */
	val startOfValidity: Long? = null, // yyyyMMdd
	/** The end of the validity period (yyyyMMdd). */
	val endOfValidity: Long? = null, // yyyyMMdd
	/** A predicate expression for conditional valorisation. */
	val predicate: String? = null,
	/** A list of reference integers. */
	val reference: List<Int>? = null,
	/** The total amount (reimbursement + doctor supplement + intervention). */
	val totalAmount: Double? = null, // =reimbursement+doctorSupplement+intervention
	/** The reimbursement amount. */
	val reimbursement: Double? = null,
	/** The patient intervention amount. */
	val patientIntervention: Double? = null,
	/** The doctor supplement amount. */
	val doctorSupplement: Double? = null,
	/** The VAT amount. */
	val vat: Double? = null,
	/** Localized labels for this valorisation, keyed by language code. */
	val label: Map<String, String>? = null, // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}
	/** The base64-encoded encrypted content. */
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable
