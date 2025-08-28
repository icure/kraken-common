/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v1.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DataAttachmentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DeletedAttachmentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DocumentLocationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DocumentStatusDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DocumentTypeDto
import org.taktik.icure.services.external.rest.v1.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v1.dto.embed.SecurityMetadataDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents a Document. It is serialized in JSON and saved in the underlying CouchDB database.""",
)
data class DocumentDto(
	@param:Schema(description = "The Id of the document. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@param:Schema(description = "The revision of the document in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	override val endOfLife: Long? = null,
	override val deletionDate: Long? = null,
	@param:Schema(description = "Location of the document") val documentLocation: DocumentLocationDto? = null,
	@param:Schema(description = "The type of document, ex: admission, clinical path, document report,invoice, etc.") val documentType: DocumentTypeDto? = null,
	@param:Schema(description = "The status of the development of the document. Ex: Draft, finalized, reviewed, signed, etc.") val documentStatus: DocumentStatusDto? = null,
	@param:Schema(
		description = "When the document is stored in an external repository, this is the uri of the document in that repository",
	) val externalUri: String? = null,
	@param:Schema(description = "Name of the document") val name: String? = null,
	@param:Schema(description = "The document version") val version: String? = null,
	@param:Schema(description = "The ICureDocument (Form, Contact, ...) that has been used to generate the document") val storedICureDocumentId: String? = null, // The ICureDocumentDto (FormDto, ContactDto, ...) that has been used to generate the document
	@param:Schema(description = "A unique external id (from another external source).") val externalUuid: String? = null,
	@param:Schema(description = "Size of the document file") val size: Long? = null,
	@param:Schema(description = "Hashed version of the document") val hash: String? = null,
	@param:Schema(description = "Id of the contact during which the document was created") val openingContactId: String? = null,
	@param:Schema(description = "Id of the main attachment of this document, if stored as a couchdb attachment") val attachmentId: String? = null,
	@param:Schema(description = "Id of the main attachment of this document, if stored using the object storage service") val objectStoreReference: String? = null,
	@param:Schema(
		description = "The main Uniform Type Identifier for the main attachment (https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_conc/understand_utis_conc.html#//apple_ref/doc/uid/TP40001319-CH202-CHDHIJDE)",
	) val mainUti: String? = null,
	@param:Schema(description = "Extra Uniform Type Identifiers for the main attachment") val otherUtis: Set<String> = emptySet(),
	@param:Schema(description = "Secondary attachments for this document") val secondaryAttachments: Map<String, DataAttachmentDto> = emptyMap(),
	@param:Schema(description = "Information on past attachments for this document") val deletedAttachments: List<DeletedAttachmentDto> =
		emptyList(),
	@param:Schema(type = "string", format = "byte") val encryptedAttachment: ByteArray? = null,
	@param:Schema(type = "string", format = "byte") val decryptedAttachment: ByteArray? = null,
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptedSelf: String? = null,
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
