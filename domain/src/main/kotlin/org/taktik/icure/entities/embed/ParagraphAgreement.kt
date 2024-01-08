/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ParagraphAgreement(
	val timestamp: Long? = null,
	val paragraph: String? = null,
	val accepted: Boolean? = null,
	val inTreatment: Boolean? = null,
	val canceled: Boolean? = null,
	val careProviderReference: String? = null,
	val decisionReference: String? = null,
	val start: Long? = null,
	val end: Long? = null,
	val cancelationDate: Long? = null,
	val quantityValue: Double? = null,
	val quantityUnit: String? = null,
	val ioRequestReference: String? = null,
	val responseType: String? = null,
	val refusalJustification: Map<String, String>? = null,
	val verses: Set<Long>? = null,
	val coverageType: String? = null,
	val unitNumber: Double? = null,
	val strength: Double? = null,
	val strengthUnit: String? = null,
	val agreementAppendices: List<AgreementAppendix>? = null,
	val documentId: String? = null
) : Serializable
