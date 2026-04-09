/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowItem(
	/** The unique identifier of this flow item. */
	val id: String? = null,
	/** The title or summary of the flow item. */
	val title: String? = null,
	/** A comment associated with the flow item. */
	val comment: String? = null,
	/** The timestamp (unix epoch in ms) when the patient was received. */
	val receptionDate: Long? = null,
	/** The timestamp (unix epoch in ms) when processing started. */
	val processingDate: Long? = null,
	/** The identifier of the person processing this flow item. */
	val processer: String? = null,
	/** The timestamp (unix epoch in ms) when this flow item was cancelled. */
	val cancellationDate: Long? = null,
	/** The identifier of the person who cancelled this flow item. */
	val canceller: String? = null,
	/** The reason for cancellation. */
	val cancellationReason: String? = null,
	/** Additional notes about the cancellation. */
	val cancellationNote: String? = null,
	/** The current status of the flow item. */
	val status: String? = null,
	/** Whether this flow item represents a home visit. */
	val homeVisit: Boolean? = null,
	/** The municipality for the visit location. */
	val municipality: String? = null,
	/** The town for the visit location. */
	val town: String? = null,
	/** The postal code for the visit location. */
	val zipCode: String? = null,
	/** The street name for the visit location. */
	val street: String? = null,
	/** The building name for the visit location. */
	val building: String? = null,
	/** The building number for the visit location. */
	val buildingNumber: String? = null,
	/** The doorbell name at the visit location. */
	val doorbellName: String? = null,
	/** The floor at the visit location. */
	val floor: String? = null,
	/** The letter box identifier at the visit location. */
	val letterBox: String? = null,
	/** Operational notes. */
	val notesOps: String? = null,
	/** Contact notes. */
	val notesContact: String? = null,
	/** The latitude coordinate of the visit location. */
	val latitude: String? = null,
	/** The longitude coordinate of the visit location. */
	val longitude: String? = null,
	/** The type of flow item. */
	val type: String? = null,
	/** Whether this is an emergency visit. */
	val emergency: Boolean? = null,
	/** The phone number of the patient. */
	val phoneNumber: String? = null,
	/** The identifier of the patient. */
	val patientId: String? = null,
	/** The last name of the patient. */
	val patientLastName: String? = null,
	/** The first name of the patient. */
	val patientFirstName: String? = null,
	/** A description of the flow item. */
	val description: String? = null,
	/** The intervention code associated with the flow item. */
	val interventionCode: String? = null,
)
