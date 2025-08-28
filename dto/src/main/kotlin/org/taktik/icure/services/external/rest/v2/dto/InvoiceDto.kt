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
import com.fasterxml.jackson.annotation.JsonInclude
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents an Invoice. It is serialized in JSON and saved in the underlying iCure CouchDB database.""",
)
data class InvoiceDto(
	@param:Schema(description = "The Id of the Invoice. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@param:Schema(
		description = "The revision of the invoice in the database, used for conflict management / optimistic locking.",
	) override val rev: String? = null,
	val identifier: List<IdentifierDto> = emptyList(),
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	override val endOfLife: Long? = null,
	override val deletionDate: Long? = null,
	@param:Schema(
		description = "The timestamp (unix epoch in ms) when the invoice was drafted, will be filled automatically if missing. Not enforced by the application server.",
	) val invoiceDate: Long? = null, // yyyyMMdd
	@param:Schema(
		description = "The timestamp (unix epoch in ms) when the invoice was sent, will be filled automatically if missing. Not enforced by the application server.",
	) val sentDate: Long? = null,
	@param:Schema(
		description = "The timestamp (unix epoch in ms) when the invoice is printed, will be filled automatically if missing. Not enforced by the application server.",
	) val printedDate: Long? = null,
	val invoicingCodes: List<InvoicingCodeDto> = emptyList(),
	@param:Schema(description = "") val receipts: Map<String, String> = emptyMap(),
	@param:Schema(description = "The type of user that receives the invoice, a patient or a healthcare party") val recipientType: String? = null, // org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto,
	// org.taktik.icure.services.external.rest.v2.dto.InsuranceDto, org.taktik.icure.services.external.rest.v2.dto.PatientDto
	@param:Schema(
		description = "Id of the recipient of the invoice. For healthcare party and insurance, patient link happens through secretForeignKeys",
	) val recipientId: String? = null, // for hcps and insurance, patient link happens through secretForeignKeys
	val invoiceReference: String? = null,
	val decisionReference: String? = null,
	val thirdPartyReference: String? = null,
	val thirdPartyPaymentJustification: String? = null,
	val thirdPartyPaymentReason: String? = null,
	val reason: String? = null,
	@param:Schema(
		description = "The format the invoice should follow based on the recipient which could be a patient, mutual fund or paying agency such as the CPAS",
	) val invoiceType: InvoiceTypeDto? = null,
	@param:Schema(description = "Medium of the invoice: CD ROM, Email, paper, etc.") val sentMediumType: MediumTypeDto? = null,
	val interventionType: InvoiceInterventionTypeDto? = null,
	val groupId: String? = null,
	@param:Schema(description = "Type of payment, ex: cash, wired, insurance, debit card, etc.") val paymentType: PaymentTypeDto? = null,
	val paid: Double? = null,
	val payments: List<PaymentDto>? = null,
	val gnotionNihii: String? = null,
	val gnotionSsin: String? = null,
	val gnotionLastName: String? = null,
	val gnotionFirstName: String? = null,
	val gnotionCdHcParty: String? = null,
	val invoicePeriod: Int? = null,
	val careProviderType: String? = null,
	val internshipNihii: String? = null,
	val internshipSsin: String? = null,
	val internshipLastName: String? = null,
	val internshipFirstName: String? = null,
	val internshipCdHcParty: String? = null,
	val internshipCbe: String? = null,
	val supervisorNihii: String? = null,
	val supervisorSsin: String? = null,
	val supervisorLastName: String? = null,
	val supervisorFirstName: String? = null,
	val supervisorCdHcParty: String? = null,
	val supervisorCbe: String? = null,
	val error: String? = null,
	val encounterLocationName: String? = null,
	val encounterLocationNihii: String? = null,
	val encounterLocationNorm: Int? = null,
	val longDelayJustification: Int? = null,
	val correctiveInvoiceId: String? = null,
	val correctedInvoiceId: String? = null,
	val creditNote: Boolean? = null,
	val creditNoteRelatedInvoiceId: String? = null,
	val idDocument: IdentityDocumentReaderDto? = null,
	// efact hospitalization
	val admissionDate: Long? = null,
	val locationNihii: String? = null,
	val locationService: Int? = null,
	// eattest cancel
	val cancelReason: String? = null,
	val cancelDate: Long? = null,
	val options: Map<String, String> = emptyMap(),
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptedSelf: Base64StringDto? = null,
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
