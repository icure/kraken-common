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
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a flow item in a waiting room or patient flow management system, tracking reception,
 * processing, and cancellation of patient visits including location and contact details.
 */
data class FlowItemDto(
	/** The unique identifier of this flow item. */
	@ActiveField val id: String? = null,
	/** The title or summary of the flow item. */
	@ActiveField val title: String? = null,
	/** A comment associated with the flow item. */
	@ActiveField val comment: String? = null,
	/** The timestamp (unix epoch in ms) when the patient was received. */
	@ActiveField val receptionDate: Long? = null,
	/** The timestamp (unix epoch in ms) when processing started. */
	@ActiveField val processingDate: Long? = null,
	/** The identifier of the person processing this flow item. */
	@ActiveField val processer: String? = null,
	/** The timestamp (unix epoch in ms) when this flow item was cancelled. */
	@ActiveField val cancellationDate: Long? = null,
	/** The identifier of the person who cancelled this flow item. */
	@ActiveField val canceller: String? = null,
	/** The reason for cancellation. */
	@ActiveField val cancellationReason: String? = null,
	/** Additional notes about the cancellation. */
	@ActiveField val cancellationNote: String? = null,
	/** The current status of the flow item. */
	@ActiveField val status: String? = null,
	/** Whether this flow item represents a home visit. */
	@ActiveField val homeVisit: Boolean? = null,
	/** The municipality for the visit location. */
	@ActiveField val municipality: String? = null,
	/** The town for the visit location. */
	@ActiveField val town: String? = null,
	/** The postal code for the visit location. */
	@ActiveField val zipCode: String? = null,
	/** The street name for the visit location. */
	@ActiveField val street: String? = null,
	/** The building name for the visit location. */
	@ActiveField val building: String? = null,
	/** The building number for the visit location. */
	@ActiveField val buildingNumber: String? = null,
	/** The doorbell name at the visit location. */
	@ActiveField val doorbellName: String? = null,
	/** The floor at the visit location. */
	@ActiveField val floor: String? = null,
	/** The letter box identifier at the visit location. */
	@ActiveField val letterBox: String? = null,
	/** Operational notes. */
	@ActiveField val notesOps: String? = null,
	/** Contact notes. */
	@ActiveField val notesContact: String? = null,
	/** The latitude coordinate of the visit location. */
	@ActiveField val latitude: String? = null,
	/** The longitude coordinate of the visit location. */
	@ActiveField val longitude: String? = null,
	/** The type of flow item. */
	@ActiveField val type: String? = null,
	/** Whether this is an emergency visit. */
	@ActiveField val emergency: Boolean? = null,
	/** The phone number of the patient. */
	@ActiveField val phoneNumber: String? = null,
	/** The identifier of the patient. */
	@ActiveField val patientId: String? = null,
	/** The last name of the patient. */
	@ActiveField val patientLastName: String? = null,
	/** The first name of the patient. */
	@ActiveField val patientFirstName: String? = null,
	/** A description of the flow item. */
	@ActiveField val description: String? = null,
	/** The intervention code associated with the flow item. */
	@ActiveField val interventionCode: String? = null,
)
