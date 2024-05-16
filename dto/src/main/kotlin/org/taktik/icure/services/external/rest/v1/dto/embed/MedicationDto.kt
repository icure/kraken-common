/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto

/**
 * Represents a medication.
 *
 * @param expirationDate the expiration date of the medication. Format: yyyyMMdd
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedicationDto(
	val compoundPrescription: String? = null,
	val substanceProduct: SubstanceproductDto? = null,
	val medicinalProduct: MedicinalproductDto? = null,
	val numberOfPackages: Int? = null,
	val batch: String? = null,
	val expirationDate: Long? = null,
	val instructionForPatient: String? = null,
	val instructionForReimbursement: String? = null,
	val commentForDelivery: String? = null,
	val drugRoute: String? = null, //CD-DRUG-ROUTE
	val temporality: String? = null, //CD-TEMPORALITY : chronic, acute, oneshot
	val frequency: CodeStubDto? = null, //CD-PERIODICITY
	val reimbursementReason: CodeStubDto? = null,
	val substitutionAllowed: Boolean? = null,
	val beginMoment: Long? = null,
	val endMoment: Long? = null,
	val deliveryMoment: Long? = null,
	val endExecutionMoment: Long? = null,
	val duration: DurationDto? = null,
	val renewal: RenewalDto? = null,
	val knownUsage: Boolean? = null,
	val regimen: List<RegimenItemDto>? = null,
	val posology: String? = null, // replace structured posology by text
	// Obsolete and Evil, must go away, has been removed from here because it provokes a loop in mapping val options: Map<String, ContentDto>? = null,
	val agreements: Map<String, ParagraphAgreementDto>? = null,
	val medicationSchemeIdOnSafe: String? = null,
	val medicationSchemeSafeVersion: Int? = null,
	val medicationSchemeTimeStampOnSafe: Long? = null,
	val medicationSchemeDocumentId: String? = null,
	val safeIdName: String? = null, //can be: vitalinkuri, RSWID, RSBID
	val idOnSafes: String? = null, //medicationschemeelement : value of vitalinkuri, RSBID, RSWID
	val timestampOnSafe: Long? = null, //transaction date+time
	val changeValidated: Boolean? = null, //accept change on safe
	val newSafeMedication: Boolean? = null, //new medication on safe
	val medicationUse: String? = null, //free text
	val beginCondition: String? = null, //free text
	val endCondition: String? = null, //free text
	val origin: String? = null, // regularprocess, recorded
	val medicationChanged: Boolean? = null,
	val posologyChanged: Boolean? = null,
	val suspension: List<SuspensionDto>? = null,
	val prescriptionRID: String? = null,
	val status: Int? = null
) : Serializable {
	companion object {
		const val REIMBURSED = "REIMBURSED"
		const val STATUS_NOT_SENT = 1 shl 0 //not send by recip-e
		const val STATUS_SENT = 1 shl 1 //sent by recip-e
		const val STATUS_PENDING = 1 shl 2 //not delivered to patient
		const val STATUS_DELIVERED = 1 shl 3 //delivered to patient
		const val STATUS_REVOKED = 1 shl 4 //revoked by physician
	}
}
