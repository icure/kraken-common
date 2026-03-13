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
import org.taktik.icure.SdkName
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an invoicing code within an invoice, containing tarification details, amounts, prescriber information,
 * and lifecycle status flags. Each invoicing code corresponds to a billable medical act or service.
 */
data class InvoicingCodeDto(
	/** The unique identifier of this invoicing code. */
	val id: String?,
	/** The date of the invoicing code as a long (yyyyMMdd format). */
	val dateCode: Long? = null,
	/** A logical identifier that remains stable when a code is resent to the insuring organization. */
	val logicalId: String? = null, // Stays the same when a code is resent to the IO
	/** The label of the invoicing code. */
	val label: String? = null,
	/** The identifier of the user who created this invoicing code. */
	val userId: String? = null,
	/** The identifier of the associated contact. */
	val contactId: String? = null,
	/** The identifier of the associated service. */
	val serviceId: String? = null,
	/** The identifier of the associated tarification. */
	@SdkName("pricingId")
	val tarificationId: String? = null,
	// For obsolete codes or codes not linked to a tarification
	/** The code value, for obsolete codes or codes not linked to a tarification. */
	val code: String? = null,
	/** The type of payment. */
	val paymentType: PaymentTypeDto? = null,
	/** The amount already paid. */
	val paid: Double? = null,
	/** The total amount (reimbursement + doctor supplement + intervention). */
	val totalAmount: Double? = null, // =reimbursement+doctorSupplement+intervention,
	/** The reimbursement amount. */
	val reimbursement: Double? = null,
	/** The patient intervention amount. */
	val patientIntervention: Double? = null,
	/** The AMI (insurance) intervention amount. */
	val amiIntervention: Double? = null,
	/** The doctor supplement amount. */
	val doctorSupplement: Double? = null,
	/** The convention amount (reimbursement + intervention). */
	val conventionAmount: Double? = null, // Should be reimbursement+intervention,
	/** The VAT amount. */
	val vat: Double? = null,
	/** The error message from eTarif, if any. */
	val error: String? = null, // Etarif
	// TODO... Might want to encrypt this as it could be used to identify the patient
	/** The contract identifier. */
	val contract: String? = null,
	/** The contract date. */
	val contractDate: Long? = null,
	/** The number of units. */
	val units: Int? = null,
	/** The side indicator. */
	val side: Int? = null,
	/** The time of day. */
	val timeOfDay: Int? = null,
	/** The hour when the eID was read. */
	val eidReadingHour: Int? = null,
	/** The value read from the eID. */
	val eidReadingValue: String? = null,
	/** The override code for third-party payer. */
	val override3rdPayerCode: Int? = null,
	/** The reason for overriding the third-party payer. */
	val override3rdPayerReason: String? = null,
	/** The transplantation code. */
	val transplantationCode: Int? = null,
	/** The prescriber norm code. */
	val prescriberNorm: Int? = null,
	/** The label of the product. */
	val productLabel: String? = null,
	/** The percent norm. */
	val percentNorm: Int? = null,
	/** The NIHII number of the prescriber. */
	val prescriberNihii: String? = null,
	/** A related code. */
	val relatedCode: String? = null,
	/** The prescription date (yyyyMMdd). */
	val prescriptionDate: Long? = null, // yyyyMMdd
	/** The maximum derogation number. */
	val derogationMaxNumber: Int? = null,
	/** The SSIN of the prescriber. */
	val prescriberSsin: String? = null,
	/** The last name of the prescriber. */
	val prescriberLastName: String? = null,
	/** The first name of the prescriber. */
	val prescriberFirstName: String? = null,
	/** The CD-HCPARTY code of the prescriber. */
	val prescriberCdHcParty: String? = null,
	/** The NIHII number of the location. */
	val locationNihii: String? = null,
	/** The CD-HCPARTY code of the location. */
	val locationCdHcParty: String? = null,
	/** The service code of the location. */
	val locationService: Int? = null,
	/** The admission date. */
	val admissionDate: Long? = null,
	/** Whether this code has been canceled. */
	val canceled: Boolean? = null,
	/** Whether this code has been accepted. */
	val accepted: Boolean? = null,
	/** Whether this code is pending. */
	val pending: Boolean? = null,
	/** Whether this code has been resent. */
	val resent: Boolean? = null,
	/** Whether this code has been archived. */
	val archived: Boolean? = null,
	/** Whether this code has been lost. */
	val lost: Boolean? = null,
	/** The insurance justification code. */
	val insuranceJustification: Int? = null,
	/** The reason for canceling patient intervention. */
	val cancelPatientInterventionReason: Int? = null,
	/** The status bitmask of this invoicing code. */
	val status: Long? = null,
	/** The label of the code. */
	val codeLabel: String? = null,
	/** Additional options as key-value pairs. */
	val options: Map<String, String> = emptyMap(),
	/** The base64-encoded encrypted content. */
	override val encryptedSelf: Base64StringDto? = null,
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
