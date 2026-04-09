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
import org.taktik.icure.entities.RawJson
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableRootDto
import org.taktik.icure.services.external.rest.v2.dto.embed.MessageAttachmentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.MessageReadStatusDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents a Message. It is serialized in JSON and saved in the underlying CouchDB database.""",
)
/**
 * Represents a message exchanged between healthcare parties. Messages can be used for internal communication,
 * eHealth box messages, eFact batches, and other types of healthcare-related communications.
 */
data class MessageDto(
	/** The ID of the message. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "The ID of the message. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	/** The revision of the message in the database, used for conflict management / optimistic locking. */
	@param:Schema(description = "The revision of the message in the database, used for conflict management / optimistic locking.")
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the User that created this message. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this message. */
	override val responsible: String? = null,
	/** The id of the medical location where this message was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the message as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular message. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** Address of the sender of the message. */
	@param:Schema(description = "Address of the sender of the message") val fromAddress: String? = null,
	/** ID of the healthcare party sending the message. */
	@param:Schema(description = "ID of the healthcare party sending the message") val fromHealthcarePartyId: String? = null,
	/** The id of the form linked to this message. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val formId: String? = null,
	/** Status of the message as a bitfield. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "Status of the message") val status: Int? = null,
	/** The type of user who is the recipient of this message. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The type of user who is the recipient of this message") val recipientsType: String? = null,
	/** List of IDs of healthcare parties to whom the message is addressed. */
	@param:Schema(description = "List of IDs of healthcare parties to whom the message is addressed") val recipients: Set<String> = emptySet(), // The id of the hcp whose the message is addressed to
	/** The addresses of the recipients of the message. */
	@param:Schema(description = "The address of the recipient of the message. Format is of an email address with extra domains defined for mycarenet and ehealth: (efact.mycarenet.be/eattest.mycarenet.be/chapter4.mycarenet.be/ehbox.ehealth.fgov.be)")
	val toAddresses: Set<String> = emptySet(),
	/** The timestamp (unix epoch in ms) when the message was received. */
	@param:Schema(description = "The timestamp (unix epoch in ms) when the message was received") val received: Long? = null,
	/** The timestamp (unix epoch in ms) when the message was sent. */
	@param:Schema(description = "The timestamp (unix epoch in ms) when the message was sent") val sent: Long? = null,
	/** Additional metadata for the message. */
	val metas: Map<String, String> = emptyMap(),
	/** Status showing whether the message is read or not and the time of reading. */
	@param:Schema(description = "Status showing whether the message is read or not and the time of reading") val readStatus: Map<String, MessageReadStatusDto> = emptyMap(),
	/** List of message attachments. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val messageAttachments: List<MessageAttachmentDto> = emptyList(),
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
	/** Transport-level identifier for the message, format depends on the transport type. */
	val transportGuid: String? = null, // Each message should have a transportGuid: see above for formats
	/** An additional remark on the message. */
	val remark: String? = null,
	/** The guid of the conversation this message belongs to. */
	val conversationGuid: String? = null,
	/** Subject for the message. */
	@param:Schema(description = "Subject for the message") val subject: String? = null,
	/** Set of IDs for invoices in the message. */
	@param:Schema(description = "Set of IDs for invoices in the message") val invoiceIds: Set<String> = emptySet(),
	/** ID of a parent in a message conversation. */
	@param:Schema(description = "ID of a parent in a message conversation") val parentId: String? = null, // ID of parent in a message conversation
	/** External reference for the message. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val externalRef: String? = null,
	/** Set of unassigned result references. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val unassignedResults: Set<String> = emptySet(), // refs
	/** Map of assigned results (ContactId to reference). */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val assignedResults: Map<String, String> = emptyMap(), // ContactId -> ref
	/** Map of sender references. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val senderReferences: Map<String, String> = emptyMap(),

	/** Extra properties for the message. */
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val properties: Set<PropertyStubDto> = emptySet(),
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
	override val extensions: RawJson.JsonObject? = null,
	override val extensionsVersion: Int? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEncryptionMetadataDto,
	EncryptableDto,
	ExtendableRootDto {
	companion object {
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
	}

	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
