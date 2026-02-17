/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.MessageAttachment
import org.taktik.icure.entities.embed.MessageReadStatus
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

/**
 * A Message
 * This entity is a root level object. It represents a Message. It is serialized in JSON and saved in the underlying CouchDB database.

 * A Message conforms to a series of interfaces:
 * - StoredICureDocument
 * - Person
 * - Encryptable
 * - CryptoActor
 *
 * @property id The ID of the message. We encourage using either a v4 UUID or a HL7 Id.
 * @property rev The revision of the message in the database, used for conflict management / optimistic locking.
 * @property created The timestamp (unix epoch in ms) of creation of the message, will be filled automatically if missing. Not enforced by the application server.
 * @property modified The date (unix epoch in ms) of the latest modification of the message, will be filled automatically if missing. Not enforced by the application server.
 * @property author The ID of the user that has created this message, will be filled automatically if missing. Not enforced by the application server.
 * @property responsible The ID of the HealthcareParty that is responsible for this message, will be filled automatically if missing. Not enforced by the application server.
 * @property medicalLocationId The ID of the medical location where this entity was created.
 * @property tags Tags that qualify the message as being member of a certain class.
 * @property codes Codes that identify or qualify this particular message.
 * @property endOfLife Soft delete (unix epoch in ms) timestamp of the object.
 * @property deletionDate Hard delete (unix epoch in ms) timestamp of the object.
 * @property fromAddress Address of the sender of the message
 * @property fromHealthcarePartyId ID of the healthcare party sending the message
 * @property status Status of the message
 * @property recipientsType The type of user who is the recipient of this message
 * @property recipients List of IDs of healthcare parties to whom the message is addressed
 * @property toAddresses The address of the recipient of the message. Format is of an email address with extra domains defined for mycarenet and ehealth: (efact.mycarenet.be/eattest.mycarenet.be/chapter4.mycarenet.be/ehbox.ehealth.fgov.be)
 * @property received The timestamp (unix epoch in ms) when the message was received
 * @property sent The timestamp (unix epoch in ms) when the message was sent
 * @property metas
 * @property readStatus Status showing whether the message is read or not and the time of reading
 * @property transportGuid
 * @property remark
 * @property conversationGuid
 * @property subject Subject for the message
 * @property invoiceIds Set of IDs for invoices in the message
 * @property parentId ID of a parent in a message conversation
 * @property externalRef
 * @property unassignedResults
 * @property assignedResults
 * @property senderReferences
 * @property delegations The delegations giving access to all connected healthcare information.
 * @property encryptionKeys The patient secret encryption key used to encrypt the secured properties (like note for example), encrypted for separate Crypto Actors.
 * @property encryptedSelf The encrypted fields of this Message.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Message(
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

	val fromAddress: String? = null,
	val fromHealthcarePartyId: String? = null,
	@Deprecated("Never used") val formId: String? = null,
	val status: Int? = null,
	val recipientsType: String? = null,
	val recipients: Set<String> = emptySet(), // The id of the hcp whose the message is addressed to
	val toAddresses: Set<String> = emptySet(), // The address of the recipient of the message. Format is of an email address with extra domains defined for mycarenet and ehealth: (efact.mycarenet.be/eattest.mycarenet.be/chapter4.mycarenet.be/ehbox.ehealth.fgov.be)
	val received: Long? = null,
	val sent: Long? = null,
	val metas: Map<String, String> = emptyMap(),
	val readStatus: Map<String, MessageReadStatus> = emptyMap(),
	val messageAttachments: List<MessageAttachment> = emptyList(),
	/*
        CHAP4:IN:   ${Mycarenet message ref}
        CHAP4:OUT:  ${Mycarenet message ref}
        EFACT:BATCH:${Mycarenet message ref}
        EFACT:IN:   ${Mycarenet message ref}
        EFACT:OUT:  ${Mycarenet message ref}
        GMD:IN:     ${Mycarenet message ref}
        INBOX:      ${Ehealth box message ref}
        SENTBOX:    ${Ehealth box message ref}
        BININBOX:   ${Ehealth box message ref}
        BINSENTBOX: ${Ehealth box message ref}
        REPORT:IN:  ${iCure ref}
        REPORT:OUT: ${iCure ref}
	 */
	val transportGuid: String? = null, // Each message should have a transportGuid: see above for formats
	val remark: String? = null,
	val conversationGuid: String? = null,
	val subject: String? = null,
	val invoiceIds: Set<String> = emptySet(),
	val parentId: String? = null, // ID of parent in a message conversation
	val externalRef: String? = null,
	val unassignedResults: Set<String> = emptySet(), // refs
	val assignedResults: Map<String, String> = emptyMap(), // ContactId -> ref
	val senderReferences: Map<String, String> = emptyMap(),

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
	companion object : DynamicInitializer<Message> {
		const val STATUS_LABO_RESULT = 1 shl 0
		const val STATUS_UNREAD = 1 shl 1
		const val STATUS_IMPORTANT = 1 shl 2
		const val STATUS_ENCRYPTED = 1 shl 3
		const val STATUS_HAS_ANNEX = 1 shl 4
		const val STATUS_HAS_FREE_INFORMATION = 1 shl 5
		const val STATUS_EFACT = 1 shl 6
		const val STATUS_SENT = 1 shl 7
		const val STATUS_SUBMITTED = 1 shl 8 // tack
		const val STATUS_RECEIVED = 1 shl 9 // tack
		const val STATUS_ACCEPTED_FOR_TREATMENT = 1 shl 10 // 931000
		const val STATUS_ACCEPTED = 1 shl 11 // 920098 920900 920099
		const val STATUS_REJECTED = 1 shl 12 // 920999
		const val STATUS_TACK = 1 shl 13
		const val STATUS_MASKED = 1 shl 14
		const val STATUS_FULL_SUCCESS = 1 shl 15 // 920900 920098
		const val STATUS_PARTIAL_SUCCESS = 1 shl 16 // 920900
		const val STATUS_FULL_ERROR = 1 shl 17 // 920099 920999
		const val STATUS_ANALYZED = 1 shl 18
		const val STATUS_DELETED_ON_SERVER = 1 shl 19
		const val STATUS_SHOULD_BE_DELETED_ON_SERVER = 1 shl 20
		const val STATUS_ARCHIVED = 1 shl 21
		const val STATUS_ERRORS_IN_PRELIMINARY_CONTROL = 1 shl 22
		const val STATUS_DRAFT = 1 shl 23
		const val STATUS_SCANNED = 1 shl 24
		const val STATUS_IMPORTED = 1 shl 25
		const val STATUS_TREATED = 1 shl 26
		const val STATUS_PUBLIC = 1 shl 27
	}

	fun merge(other: Message) = Message(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Message) = super<StoredICureDocument>.solveConflictsWith(other) +
		super<HasEncryptionMetadata>.solveConflictsWith(other) +
		super<Encryptable>.solveConflictsWith(other) +
		mapOf(
			"fromAddress" to (this.fromAddress ?: other.fromAddress),
			"fromHealthcarePartyId" to (this.fromHealthcarePartyId ?: other.fromHealthcarePartyId),
			"formId" to (this.formId ?: other.formId),
			"status" to (this.status ?: other.status),
			"recipientsType" to (this.recipientsType ?: other.recipientsType),
			"recipients" to (other.recipients + this.recipients),
			"toAddresses" to (other.toAddresses + this.toAddresses),
			"received" to (this.received ?: other.received),
			"sent" to (this.sent ?: other.sent),
			"metas" to (other.metas + this.metas),
			"readStatus" to (this.readStatus),
			"transportGuid" to (this.transportGuid ?: other.transportGuid),
			"remark" to (this.remark ?: other.remark),
			"conversationGuid" to (this.conversationGuid ?: other.conversationGuid),
			"subject" to (this.subject ?: other.subject),
			"invoiceIds" to (other.invoiceIds + this.invoiceIds),
			"parentId" to (this.parentId ?: other.parentId),
			"externalRef" to (this.externalRef ?: other.externalRef),
			"unassignedResults" to (other.unassignedResults + this.unassignedResults),
			"assignedResults" to (other.assignedResults + this.assignedResults),
			"senderReferences" to (other.senderReferences + this.senderReferences),
		)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
}
