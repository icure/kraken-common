/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoicingCodeDto(
	val id: String?,
	val dateCode: Long? = null,
	val logicalId: String? = null, // Stays the same when a code is resent to the IO
	val label: String? = null,
	val userId: String? = null,
	val contactId: String? = null,
	val serviceId: String? = null,
	val tarificationId: String? = null,
	// For obsolete codes or codes not linked to a tarification
	val code: String? = null,
	val paymentType: PaymentTypeDto? = null,
	val paid: Double? = null,
	val totalAmount: Double? = null, // =reimbursement+doctorSupplement+intervention,
	val reimbursement: Double? = null,
	val patientIntervention: Double? = null,
	val amiIntervention: Double? = null,
	val doctorSupplement: Double? = null,
	val conventionAmount: Double? = null, // Should be reimbursement+intervention,
	val vat: Double? = null,
	val error: String? = null, // Etarif
	// TODO... Might want to encrypt this as it could be used to identify the patient
	val contract: String? = null,
	val contractDate: Long? = null,
	val units: Int? = null,
	val side: Int? = null,
	val timeOfDay: Int? = null,
	val eidReadingHour: Int? = null,
	val eidReadingValue: String? = null,
	val override3rdPayerCode: Int? = null,
	val override3rdPayerReason: String? = null,
	val transplantationCode: Int? = null,
	val prescriberNorm: Int? = null,
	val productLabel: String? = null,
	val percentNorm: Int? = null,
	val prescriberNihii: String? = null,
	val relatedCode: String? = null,
	val prescriptionDate: Long? = null, // yyyyMMdd
	val derogationMaxNumber: Int? = null,
	val prescriberSsin: String? = null,
	val prescriberLastName: String? = null,
	val prescriberFirstName: String? = null,
	val prescriberCdHcParty: String? = null,
	val locationNihii: String? = null,
	val locationCdHcParty: String? = null,
	val locationService: Int? = null,
	val admissionDate: Long? = null,
	val canceled: Boolean? = null,
	val accepted: Boolean? = null,
	val pending: Boolean? = null,
	val resent: Boolean? = null,
	val archived: Boolean? = null,
	val lost: Boolean? = null,
	val insuranceJustification: Int? = null,
	val cancelPatientInterventionReason: Int? = null,
	val status: Long? = null,
	val codeLabel: String? = null,
	val options: Map<String, String> = emptyMap(),
	override val encryptedSelf: String? = null,
) : EncryptableDto,
	Comparable<InvoicingCodeDto?> {
	companion object {
		const val STATUS_PAID: Long = 1
		const val STATUS_PRINTED: Long = 2
		const val STATUS_PAIDPRINTED: Long = 3
		const val STATUS_PENDING: Long = 4
		const val STATUS_CANCELED: Long = 8
		const val STATUS_ACCEPTED: Long = 16
		const val STATUS_RESENT: Long = 32
		const val STATUS_LOST: Long = 64
	}

	override fun compareTo(other: InvoicingCodeDto?): Int = if (other == null) -1 else dateCode?.compareTo(other.dateCode ?: 0) ?: 0
}
