/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ValorisationDto(
	val startOfValidity: Long? = null, //yyyyMMdd
	val endOfValidity: Long? = null, //yyyyMMdd
	val predicate: String? = null,
	val reference: List<Int>? = null,
	val totalAmount: Double? = null, //=reimbursement+doctorSupplement+intervention
	val reimbursement: Double? = null,
	val patientIntervention: Double? = null,
	val doctorSupplement: Double? = null,
	val vat: Double? = null,
	val label: Map<String, String>? = emptyMap(), //ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}
	override val encryptedSelf: String? = null
) : EncryptableDto, Serializable
