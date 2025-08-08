/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowItem(
	val id: String? = null,
	val title: String? = null,
	val comment: String? = null,
	val receptionDate: Long? = null,
	val processingDate: Long? = null,
	val processer: String? = null,
	val cancellationDate: Long? = null,
	val canceller: String? = null,
	val cancellationReason: String? = null,
	val cancellationNote: String? = null,
	val status: String? = null,
	val homeVisit: Boolean? = null,
	val municipality: String? = null,
	val town: String? = null,
	val zipCode: String? = null,
	val street: String? = null,
	val building: String? = null,
	val buildingNumber: String? = null,
	val doorbellName: String? = null,
	val floor: String? = null,
	val letterBox: String? = null,
	val notesOps: String? = null,
	val notesContact: String? = null,
	val latitude: String? = null,
	val longitude: String? = null,
	val type: String? = null,
	val emergency: Boolean? = null,
	val phoneNumber: String? = null,
	val patientId: String? = null,
	val patientLastName: String? = null,
	val patientFirstName: String? = null,
	val description: String? = null,
	val interventionCode: String? = null,
)
