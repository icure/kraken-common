/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.couchdb.id.UUIDGenerator
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.IdentityDocumentReader
import org.taktik.icure.entities.embed.InvoiceInterventionType
import org.taktik.icure.entities.embed.InvoiceType
import org.taktik.icure.entities.embed.InvoicingCode
import org.taktik.icure.entities.embed.MediumType
import org.taktik.icure.entities.embed.Payment
import org.taktik.icure.entities.embed.PaymentType
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class Invoice(
	/** The Id of the Invoice. We encourage using either a v4 UUID or a HL7 Id. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the invoice in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	/** The id of the User that created this invoice. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this invoice. */
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	/** The id of the medical location where this invoice was created. */
	override val medicalLocationId: String? = null,
	/** Tags that qualify the invoice as being member of a certain class. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	/** Codes that identify or qualify this particular invoice. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The identifiers of the invoice. */
	val identifier: List<Identifier> = listOf(),

	/** The timestamp (unix epoch in ms) when the invoice was drafted, will be filled automatically if missing. Not enforced by the application server. */
	val invoiceDate: Long? = null, // yyyyMMdd
	/** The timestamp (unix epoch in ms) when the invoice was sent, will be filled automatically if missing. Not enforced by the application server. */
	val sentDate: Long? = null,
	/** The timestamp (unix epoch in ms) when the invoice was printed, will be filled automatically if missing. Not enforced by the application server. */
	val printedDate: Long? = null,
	/** The list of invoicing codes associated with this invoice. */
	val invoicingCodes: List<InvoicingCode> = emptyList(),
	/** Map of receipt references. */
	val receipts: Map<String, String> = emptyMap(),
	/** The type of user who is the recipient of this invoice (patient or healthcare party). */
	val recipientType: String? = null, // org.taktik.icure.entities.HealthcareParty,

	/** The decision reference number. */
	val decisionReference: String? = null,
	/** Id of the recipient of the invoice. For healthcare party and insurance, patient link happens through secretForeignKeys. */
	// org.taktik.icure.entities.Insurance, org.taktik.icure.entities.Patient
	val recipientId: String? = null, // for hcps and insurance, patient link happens through secretForeignKeys
	/** The invoice reference number. */
	val invoiceReference: String? = null,
	/** The third party reference. */
	val thirdPartyReference: String? = null,
	/** Justification for third party payment. */
	val thirdPartyPaymentJustification: String? = null,
	/** Reason for third party payment. */
	val thirdPartyPaymentReason: String? = null,
	/** The reason for the invoice. */
	val reason: String? = null,
	/** The format the invoice should follow based on the recipient. */
	val invoiceType: InvoiceType? = null,
	/** Medium of the invoice: CD ROM, Email, paper, etc. */
	val sentMediumType: MediumType? = null,
	/** The type of intervention. */
	val interventionType: InvoiceInterventionType? = null,
	/** The group id for grouping related invoices. */
	val groupId: String? = null,
	/** Type of payment (cash, wired, insurance, debit card, etc.). */
	val paymentType: PaymentType? = null,
	/** The amount paid. */
	val paid: Double? = null,
	/** List of payments made for this invoice. */
	val payments: List<Payment>? = null,
	/** NIHII number of the gnotion. */
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
	val idDocument: IdentityDocumentReader? = null,

	/** The admission date for hospitalization invoices. */
	// efact hospitalization
	val admissionDate: Long? = null,
	/** NIHII number of the location. */
	val locationNihii: String? = null,
	/** Service code of the location. */
	val locationService: Int? = null,

	/** The reason for cancellation. */
	// eattest cancel
	val cancelReason: String? = null,
	/** The date of cancellation. */
	val cancelDate: Long? = null,

	/** Extra options for the invoice. */
	val options: Map<String, String> = emptyMap(),

	/** The secret patient key, encrypted in the patient's own AES key. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The patient id encrypted in the delegates' AES keys. */
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	/** The keys used to encrypt this entity when stored encrypted. */
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The base64-encoded encrypted fields of this entity. */
	override val encryptedSelf: String? = null,
	/** The security metadata of the entity. */
	override val securityMetadata: SecurityMetadata? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredICureDocument,
	HasEncryptionMetadata,
	Encryptable {

	fun reassign(invoicingCodes: List<InvoicingCode>, uuidGenerator: UUIDGenerator) = this.copy(
		id = uuidGenerator.newGUID().toString(),
		created = System.currentTimeMillis(),
		invoicingCodes = invoicingCodes.map { ic ->
			ic.copy(
				id = uuidGenerator.newGUID().toString(),
				resent = true,
				canceled = false,
				pending = false,
				accepted = false,
			)
		},
	)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
	override fun withEncryptionMetadata(
		secretForeignKeys: Set<String>,
		cryptedForeignKeys: Map<String, Set<Delegation>>,
		delegations: Map<String, Set<Delegation>>,
		encryptionKeys: Map<String, Set<Delegation>>,
		securityMetadata: SecurityMetadata?
	) = copy(
		secretForeignKeys = secretForeignKeys,
		cryptedForeignKeys = cryptedForeignKeys,
		delegations = delegations,
		encryptionKeys = encryptionKeys,
		securityMetadata = securityMetadata
	)
}
