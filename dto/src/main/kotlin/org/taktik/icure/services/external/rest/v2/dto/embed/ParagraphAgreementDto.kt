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
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an agreement on a specific paragraph of a regulation, including approval status,
 * validity period, quantities, and any refusal justification.
 */
data class ParagraphAgreementDto(
	/** The timestamp (unix epoch in ms) of the agreement. */
	@ActiveField val timestamp: Long? = null,
	/** The paragraph reference. */
	@ActiveField val paragraph: String? = null,
	/** Whether the agreement was accepted. */
	@ActiveField val accepted: Boolean? = null,
	/** Whether the patient is currently in treatment. */
	@ActiveField val inTreatment: Boolean? = null,
	/** Whether the agreement was canceled. */
	@ActiveField val canceled: Boolean? = null,
	/** The care provider reference for this agreement. */
	@ActiveField val careProviderReference: String? = null,
	/** The decision reference. */
	@ActiveField val decisionReference: String? = null,
	/** The start date of the agreement period. */
	@ActiveField val start: Long? = null,
	/** The end date of the agreement period. */
	@ActiveField val end: Long? = null,
	/** The date of cancellation. */
	@ActiveField val cancelationDate: Long? = null,
	/** The quantity value of the agreement. */
	@ActiveField val quantityValue: Double? = null,
	/** The unit of the quantity. */
	@ActiveField val quantityUnit: String? = null,
	/** The IO request reference. */
	@ActiveField val ioRequestReference: String? = null,
	/** The type of response received. */
	@ActiveField val responseType: String? = null,
	/** Localized justification for refusal, keyed by language code. */
	@ActiveField val refusalJustification: Map<String, String>? = null,
	/** The set of verse numbers covered by this agreement. */
	@ActiveField val verses: Set<Long>? = null,
	/** The type of coverage. */
	@ActiveField val coverageType: String? = null,
	/** The number of units. */
	@ActiveField val unitNumber: Double? = null,
	/** The strength value. */
	@ActiveField val strength: Double? = null,
	/** The unit of strength. */
	@ActiveField val strengthUnit: String? = null,
	/** The list of agreement appendices. */
	@ActiveField val agreementAppendices: List<AgreementAppendixDto>? = null,
	/** The identifier of the associated document. */
	@ActiveField val documentId: String? = null,
) : Serializable
