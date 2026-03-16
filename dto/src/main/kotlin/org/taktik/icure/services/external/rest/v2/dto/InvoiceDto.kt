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
package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.IdentityDocumentReaderDto
import org.taktik.icure.services.external.rest.v2.dto.embed.InvoiceInterventionTypeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.InvoiceTypeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.InvoicingCodeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.MediumTypeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PaymentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PaymentTypeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents an Invoice. It is serialized in JSON and saved in the underlying iCure CouchDB database.""",
)
/**
 * Represents an invoice. An invoice is used to bill patients, mutual funds, or paying agencies for healthcare
 * services rendered. It is serialized in JSON and saved in the underlying iCure CouchDB database.
 */
data class InvoiceDto(
	/** The Id of the Invoice. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "The Id of the Invoice. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	/** The revision of the invoice in the database, used for conflict management / optimistic locking. */
	@param:Schema(description = "The revision of the invoice in the database, used for conflict management / optimistic locking.")
	override val rev: String? = null,
	/** The identifiers of the invoice. */
	val identifier: List<IdentifierDto> = emptyList(),
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the User that created this invoice. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this invoice. */
	override val responsible: String? = null,
	/** The id of the medical location where this invoice was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the invoice as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular invoice. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The timestamp (unix epoch in ms) when the invoice was drafted. */
	@param:Schema(description = "The timestamp (unix epoch in ms) when the invoice was drafted, will be filled automatically if missing. Not enforced by the application server.")
	val invoiceDate: Long? = null, // yyyyMMdd
	/** The timestamp (unix epoch in ms) when the invoice was sent. */
	@param:Schema(description = "The timestamp (unix epoch in ms) when the invoice was sent, will be filled automatically if missing. Not enforced by the application server.")
	val sentDate: Long? = null,
	/** The timestamp (unix epoch in ms) when the invoice was printed. */
	@param:Schema(description = "The timestamp (unix epoch in ms) when the invoice is printed, will be filled automatically if missing. Not enforced by the application server.")
	val printedDate: Long? = null,
	/** The list of invoicing codes associated with this invoice. */
	val invoicingCodes: List<InvoicingCodeDto> = emptyList(),
	/** Map of receipt references. */
	@param:Schema(description = "") val receipts: Map<String, String> = emptyMap(),
	/** The type of user who is the recipient of this invoice (patient or healthcare party). */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The type of user that receives the invoice, a patient or a healthcare party") val recipientType: String? = null, // org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto,
	// org.taktik.icure.services.external.rest.v2.dto.InsuranceDto, org.taktik.icure.services.external.rest.v2.dto.PatientDto
	/** Id of the recipient of the invoice. */
	@param:Schema(description = "Id of the recipient of the invoice. For healthcare party and insurance, patient link happens through secretForeignKeys")
	val recipientId: String? = null, // for hcps and insurance, patient link happens through secretForeignKeys
	/** The invoice reference number. */
	val invoiceReference: String? = null,
	/** The decision reference number. */
	val decisionReference: String? = null,
	/** The third party reference. */
	val thirdPartyReference: String? = null,
	/** Justification for third party payment. */
	val thirdPartyPaymentJustification: String? = null,
	/** Reason for third party payment. */
	val thirdPartyPaymentReason: String? = null,
	/** The reason for the invoice. */
	val reason: String? = null,
	/** The format the invoice should follow based on the recipient. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The format the invoice should follow based on the recipient which could be a patient, mutual fund or paying agency such as the CPAS")
	val invoiceType: InvoiceTypeDto? = null,
	/** Medium of the invoice: CD ROM, Email, paper, etc. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "Medium of the invoice: CD ROM, Email, paper, etc.")
	val sentMediumType: MediumTypeDto? = null,
	/** The type of intervention. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val interventionType: InvoiceInterventionTypeDto? = null,
	/** The group id for grouping related invoices. */
	val groupId: String? = null,
	/** Type of payment (cash, wired, insurance, debit card, etc.). */
	@param:Schema(description = "Type of payment, ex: cash, wired, insurance, debit card, etc.")
	val paymentType: PaymentTypeDto? = null,
	/** The amount paid. */
	val paid: Double? = null,
	/** List of payments made for this invoice. */
	val payments: List<PaymentDto>? = null,
	/** NIHII number of the gnotion. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val gnotionNihii: String? = null,
	/** SSIN of the gnotion. */
	val gnotionSsin: String? = null,
	/** Last name of the gnotion. */
	val gnotionLastName: String? = null,
	/** First name of the gnotion. */
	val gnotionFirstName: String? = null,
	/** CD-HCPARTY code of the gnotion. */
	val gnotionCdHcParty: String? = null,
	/** The invoicing period. */
	val invoicePeriod: Int? = null,
	/** The type of care provider. */
	val careProviderType: String? = null,
	/** NIHII number of the internship. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val internshipNihii: String? = null,
	/** SSIN of the internship. */
	val internshipSsin: String? = null,
	/** Last name of the internship supervisor. */
	val internshipLastName: String? = null,
	/** First name of the internship supervisor. */
	val internshipFirstName: String? = null,
	/** CD-HCPARTY code of the internship. */
	val internshipCdHcParty: String? = null,
	/** CBE number of the internship. */
	val internshipCbe: String? = null,
	/** NIHII number of the supervisor. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val supervisorNihii: String? = null,
	/** SSIN of the supervisor. */
	val supervisorSsin: String? = null,
	/** Last name of the supervisor. */
	val supervisorLastName: String? = null,
	/** First name of the supervisor. */
	val supervisorFirstName: String? = null,
	/** CD-HCPARTY code of the supervisor. */
	val supervisorCdHcParty: String? = null,
	/** CBE number of the supervisor. */
	val supervisorCbe: String? = null,
	/** Error message if any. */
	val error: String? = null,
	/** Name of the encounter location. */
	val encounterLocationName: String? = null,
	/** NIHII number of the encounter location. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val encounterLocationNihii: String? = null,
	/** Norm of the encounter location. */
	val encounterLocationNorm: Int? = null,
	/** Justification for long delay. */
	val longDelayJustification: Int? = null,
	/** Id of the corrective invoice. */
	val correctiveInvoiceId: String? = null,
	/** Id of the corrected invoice. */
	val correctedInvoiceId: String? = null,
	/** Whether this invoice is a credit note. */
	val creditNote: Boolean? = null,
	/** Id of the related invoice for the credit note. */
	val creditNoteRelatedInvoiceId: String? = null,
	/** Identity document reader information. */
	val idDocument: IdentityDocumentReaderDto? = null,
	// efact hospitalization
	/** The admission date for hospitalization invoices. */
	val admissionDate: Long? = null,
	/** NIHII number of the location. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val locationNihii: String? = null,
	/** Service code of the location. */
	val locationService: Int? = null,
	// eattest cancel
	/** The reason for cancellation. */
	val cancelReason: String? = null,
	/** The date of cancellation. */
	val cancelDate: Long? = null,
	/** Extra options for the invoice. */
	val options: Map<String, String> = emptyMap(),
	/** The secret patient key, encrypted in the patient's own AES key. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The patient id encrypted in the delegates' AES keys. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The keys used to encrypt this entity when stored encrypted. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The base64-encoded encrypted fields of this entity. */
	override val encryptedSelf: Base64StringDto? = null,
	/** The security metadata of the entity. */
	override val securityMetadata: SecurityMetadataDto? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEncryptionMetadataDto,
	EncryptableDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
