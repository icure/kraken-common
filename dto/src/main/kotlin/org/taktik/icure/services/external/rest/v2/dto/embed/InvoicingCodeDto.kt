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
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents an invoicing code within an invoice, containing tarification details, amounts, prescriber information,
 * and lifecycle status flags. Each invoicing code corresponds to a billable medical act or service.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.InvoicingCodeDto")
data class InvoicingCodeDto(
	/** The unique identifier of this invoicing code. */
	@ActiveField val id: String?,
	/** The date of the invoicing code as a long (yyyyMMdd format). */
	@ActiveField val dateCode: Long? = null,
	/** A logical identifier that remains stable when a code is resent to the insuring organization. */
	@ActiveField val logicalId: String? = null, // Stays the same when a code is resent to the IO
	/** The label of the invoicing code. */
	@ActiveField val label: String? = null,
	/** The identifier of the user who created this invoicing code. */
	@ActiveField val userId: String? = null,
	/** The identifier of the associated contact. */
	@ActiveField val contactId: String? = null,
	/** The identifier of the associated service. */
	@ActiveField val serviceId: String? = null,
	/** The identifier of the associated tarification. */
	@SdkName("pricingId")
	@ActiveField val tarificationId: String? = null,
	// For obsolete codes or codes not linked to a tarification
	/** The code value, for obsolete codes or codes not linked to a tarification. */
	@ActiveField val code: String? = null,
	/** The type of payment. */
	@ActiveField val paymentType: PaymentTypeDto? = null,
	/** The amount already paid. */
	@ActiveField val paid: Double? = null,
	/** The total amount (reimbursement + doctor supplement + intervention). */
	@ActiveField val totalAmount: Double? = null, // =reimbursement+doctorSupplement+intervention,
	/** The reimbursement amount. */
	@ActiveField val reimbursement: Double? = null,
	/** The patient intervention amount. */
	@ActiveField val patientIntervention: Double? = null,
	/** The AMI (insurance) intervention amount. */
	@ActiveField val amiIntervention: Double? = null,
	/** The doctor supplement amount. */
	@ActiveField val doctorSupplement: Double? = null,
	/** The convention amount (reimbursement + intervention). */
	@ActiveField val conventionAmount: Double? = null, // Should be reimbursement+intervention,
	/** The VAT amount. */
	@ActiveField val vat: Double? = null,
	/** The error message from eTarif, if any. */
	@ActiveField val error: String? = null, // Etarif
	// TODO... Might want to encrypt this as it could be used to identify the patient
	/** The contract identifier. */
	@ActiveField val contract: String? = null,
	/** The contract date. */
	@ActiveField val contractDate: Long? = null,
	/** The number of units. */
	@ActiveField val units: Int? = null,
	/** The side indicator. */
	@ActiveField val side: Int? = null,
	/** The time of day. */
	@ActiveField val timeOfDay: Int? = null,
	/** The hour when the eID was read. */
	@ActiveField val eidReadingHour: Int? = null,
	/** The value read from the eID. */
	@ActiveField val eidReadingValue: String? = null,
	/** The override code for third-party payer. */
	@ActiveField val override3rdPayerCode: Int? = null,
	/** The reason for overriding the third-party payer. */
	@ActiveField val override3rdPayerReason: String? = null,
	/** The transplantation code. */
	@ActiveField val transplantationCode: Int? = null,
	/** The prescriber norm code. */
	@ActiveField val prescriberNorm: Int? = null,
	/** The label of the product. */
	@ActiveField val productLabel: String? = null,
	/** The percent norm. */
	@ActiveField val percentNorm: Int? = null,
	/** The NIHII number of the prescriber. */
	@ActiveField val prescriberNihii: String? = null,
	/** A related code. */
	@ActiveField val relatedCode: String? = null,
	/** The prescription date (yyyyMMdd). */
	@ActiveField val prescriptionDate: Long? = null, // yyyyMMdd
	/** The maximum derogation number. */
	@ActiveField val derogationMaxNumber: Int? = null,
	/** The SSIN of the prescriber. */
	@ActiveField val prescriberSsin: String? = null,
	/** The last name of the prescriber. */
	@ActiveField val prescriberLastName: String? = null,
	/** The first name of the prescriber. */
	@ActiveField val prescriberFirstName: String? = null,
	/** The CD-HCPARTY code of the prescriber. */
	@ActiveField val prescriberCdHcParty: String? = null,
	/** The NIHII number of the location. */
	@ActiveField val locationNihii: String? = null,
	/** The CD-HCPARTY code of the location. */
	@ActiveField val locationCdHcParty: String? = null,
	/** The service code of the location. */
	@ActiveField val locationService: Int? = null,
	/** The admission date. */
	@ActiveField val admissionDate: Long? = null,
	/** Whether this code has been canceled. */
	@ActiveField val canceled: Boolean? = null,
	/** Whether this code has been accepted. */
	@ActiveField val accepted: Boolean? = null,
	/** Whether this code is pending. */
	@ActiveField val pending: Boolean? = null,
	/** Whether this code has been resent. */
	@ActiveField val resent: Boolean? = null,
	/** Whether this code has been archived. */
	@ActiveField val archived: Boolean? = null,
	/** Whether this code has been lost. */
	@ActiveField val lost: Boolean? = null,
	/** The insurance justification code. */
	@ActiveField val insuranceJustification: Int? = null,
	/** The reason for canceling patient intervention. */
	@ActiveField val cancelPatientInterventionReason: Int? = null,
	/** The status bitmask of this invoicing code. */
	@ActiveField val status: Long? = null,
	/** The label of the code. */
	@ActiveField val codeLabel: String? = null,
	/** Additional options as key-value pairs. */
	@ActiveField val options: Map<String, String> = emptyMap(),
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
