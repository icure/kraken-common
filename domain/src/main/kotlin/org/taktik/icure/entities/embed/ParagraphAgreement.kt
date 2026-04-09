/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ParagraphAgreement(
	/** The timestamp (unix epoch in ms) of the agreement. */
	val timestamp: Long? = null,
	/** The paragraph reference. */
	val paragraph: String? = null,
	/** Whether the agreement was accepted. */
	val accepted: Boolean? = null,
	/** Whether the patient is currently in treatment. */
	val inTreatment: Boolean? = null,
	/** Whether the agreement was canceled. */
	val canceled: Boolean? = null,
	/** The care provider reference for this agreement. */
	val careProviderReference: String? = null,
	/** The decision reference. */
	val decisionReference: String? = null,
	/** The start date of the agreement period. */
	val start: Long? = null,
	/** The end date of the agreement period. */
	val end: Long? = null,
	/** The date of cancellation. */
	val cancelationDate: Long? = null,
	/** The quantity value of the agreement. */
	val quantityValue: Double? = null,
	/** The unit of the quantity. */
	val quantityUnit: String? = null,
	/** The IO request reference. */
	val ioRequestReference: String? = null,
	/** The type of response received. */
	val responseType: String? = null,
	/** Localized justification for refusal, keyed by language code. */
	val refusalJustification: Map<String, String>? = null,
	/** The set of verse numbers covered by this agreement. */
	val verses: Set<Long>? = null,
	/** The type of coverage. */
	val coverageType: String? = null,
	/** The number of units. */
	val unitNumber: Double? = null,
	/** The strength value. */
	val strength: Double? = null,
	/** The unit of strength. */
	val strengthUnit: String? = null,
	/** The list of agreement appendices. */
	val agreementAppendices: List<AgreementAppendix>? = null,
	/** The identifier of the associated document. */
	val documentId: String? = null,
) : Serializable
