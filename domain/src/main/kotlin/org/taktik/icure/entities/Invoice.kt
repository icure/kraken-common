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
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	val identifier: List<Identifier> = listOf(),

	val invoiceDate: Long? = null, // yyyyMMdd
	val sentDate: Long? = null,
	val printedDate: Long? = null,
	val invoicingCodes: List<InvoicingCode> = emptyList(),
	val receipts: Map<String, String> = emptyMap(),
	val recipientType: String? = null, // org.taktik.icure.entities.HealthcareParty,

	val decisionReference: String? = null,
	// org.taktik.icure.entities.Insurance, org.taktik.icure.entities.Patient
	val recipientId: String? = null, // for hcps and insurance, patient link happens through secretForeignKeys
	val invoiceReference: String? = null,
	val thirdPartyReference: String? = null,
	val thirdPartyPaymentJustification: String? = null,
	val thirdPartyPaymentReason: String? = null,
	val reason: String? = null,
	val invoiceType: InvoiceType? = null,
	val sentMediumType: MediumType? = null,
	val interventionType: InvoiceInterventionType? = null,
	val groupId: String? = null,
	val paymentType: PaymentType? = null,
	val paid: Double? = null,
	val payments: List<Payment>? = null,
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
	val idDocument: IdentityDocumentReader? = null,

	// efact hospitalization
	val admissionDate: Long? = null,
	val locationNihii: String? = null,
	val locationService: Int? = null,

	// eattest cancel
	val cancelReason: String? = null,
	val cancelDate: Long? = null,

	val options: Map<String, String> = emptyMap(),

	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptedSelf: String? = null,
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
