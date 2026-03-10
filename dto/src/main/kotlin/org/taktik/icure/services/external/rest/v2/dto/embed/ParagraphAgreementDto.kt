/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an agreement on a specific paragraph of a regulation, including approval status,
 * validity period, quantities, and any refusal justification.
 */
data class ParagraphAgreementDto(
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
	val agreementAppendices: List<AgreementAppendixDto>? = null,
	/** The identifier of the associated document. */
	val documentId: String? = null,
) : Serializable
