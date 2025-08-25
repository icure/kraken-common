/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class InvoicingCodeDto(
	val id: String?,
	val dateCode: Long? = null,
	val logicalId: String? = null, //Stays the same when a code is resent to the IO
	val label: String? = null,
	val userId: String? = null,
	val contactId: String? = null,
	val serviceId: String? = null,
	val tarificationId: String? = null,
	//For obsolete codes or codes not linked to a tarification
	val code: String? = null,
	val paymentType: PaymentTypeDto? = null,
	val paid: Double? = null,
	val totalAmount: Double? = null, //=reimbursement+doctorSupplement+intervention,
	val reimbursement: Double? = null,
	val patientIntervention: Double? = null,
	val amiIntervention: Double? = null,
	val doctorSupplement: Double? = null,
	val conventionAmount: Double? = null, //Should be reimbursement+intervention,
	val vat: Double? = null,
	val error: String? = null, //Etarif
	//TODO... Might want to encrypt this as it could be used to identify the patient
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
	override val encryptedSelf: String? = null
) : EncryptableDto, Comparable<InvoicingCodeDto?> {
	companion object : DynamicInitializer<InvoicingCodeDto> {
		const val STATUS_PAID: Long = 1
		const val STATUS_PRINTED: Long = 2
		const val STATUS_PAIDPRINTED: Long = 3
		const val STATUS_PENDING: Long = 4
		const val STATUS_CANCELED: Long = 8
		const val STATUS_ACCEPTED: Long = 16
		const val STATUS_RESENT: Long = 32
		const val STATUS_LOST: Long = 64
	}

	fun merge(other: InvoicingCodeDto) = InvoicingCodeDto(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: InvoicingCodeDto) = mapOf(
		"id" to (this.id),
		"dateCode" to (this.dateCode ?: other.dateCode),
		"logicalId" to (this.logicalId ?: other.logicalId),
		"label" to (this.label ?: other.label),
		"userId" to (this.userId ?: other.userId),
		"contactId" to (this.contactId ?: other.contactId),
		"serviceId" to (this.serviceId ?: other.serviceId),
		"tarificationId" to (this.tarificationId ?: other.tarificationId),
		"code" to (this.code ?: other.code),
		"paymentType" to (this.paymentType ?: other.paymentType),
		"paid" to (this.paid ?: other.paid),
		"totalAmount" to (this.totalAmount ?: other.totalAmount),
		"reimbursement" to (this.reimbursement ?: other.reimbursement),
		"patientIntervention" to (this.patientIntervention ?: other.patientIntervention),
		"amiIntervention" to (this.amiIntervention ?: other.amiIntervention),
		"doctorSupplement" to (this.doctorSupplement ?: other.doctorSupplement),
		"conventionAmount" to (this.conventionAmount ?: other.conventionAmount),
		"vat" to (this.vat ?: other.vat),
		"error" to (this.error ?: other.error),
		"contract" to (this.contract ?: other.contract),
		"contractDate" to (this.contractDate ?: other.contractDate),
		"units" to (this.units ?: other.units),
		"side" to (this.side ?: other.side),
		"timeOfDay" to (this.timeOfDay ?: other.timeOfDay),
		"eidReadingHour" to (this.eidReadingHour ?: other.eidReadingHour),
		"eidReadingValue" to (this.eidReadingValue ?: other.eidReadingValue),
		"override3rdPayerCode" to (this.override3rdPayerCode ?: other.override3rdPayerCode),
		"override3rdPayerReason" to (this.override3rdPayerReason ?: other.override3rdPayerReason),
		"transplantationCode" to (this.transplantationCode ?: other.transplantationCode),
		"prescriberNorm" to (this.prescriberNorm ?: other.prescriberNorm),
		"percentNorm" to (this.percentNorm ?: other.percentNorm),
		"prescriberNihii" to (this.prescriberNihii ?: other.prescriberNihii),
		"relatedCode" to (this.relatedCode ?: other.relatedCode),
		"prescriptionDate" to (this.prescriptionDate ?: other.prescriptionDate),
		"derogationMaxNumber" to (this.derogationMaxNumber ?: other.derogationMaxNumber),
		"prescriberSsin" to (this.prescriberSsin ?: other.prescriberSsin),
		"prescriberLastName" to (this.prescriberLastName ?: other.prescriberLastName),
		"prescriberFirstName" to (this.prescriberFirstName ?: other.prescriberFirstName),
		"prescriberCdHcParty" to (this.prescriberCdHcParty ?: other.prescriberCdHcParty),
		"locationNihii" to (this.locationNihii ?: other.locationNihii),
		"locationCdHcParty" to (this.locationCdHcParty ?: other.locationCdHcParty),
		"locationService" to (this.locationService ?: other.locationService),
		"admissionDate" to (this.admissionDate ?: other.admissionDate),
		"canceled" to (this.canceled ?: other.canceled),
		"accepted" to (this.accepted ?: other.accepted),
		"pending" to (this.pending ?: other.pending),
		"resent" to (this.resent ?: other.resent),
		"archived" to (this.archived ?: other.archived),
		"lost" to (this.lost ?: other.lost),
		"insuranceJustification" to (this.insuranceJustification ?: other.insuranceJustification),
		"cancelPatientInterventionReason" to (
			this.cancelPatientInterventionReason
				?: other.cancelPatientInterventionReason
			),
		"status" to (this.status ?: other.status),
		"codeLabel" to (this.codeLabel ?: other.codeLabel)
	)

	override fun compareTo(other: InvoicingCodeDto?): Int {
		return if (other == null) -1 else dateCode?.compareTo(other.dateCode ?: 0) ?: 0
	}
}
