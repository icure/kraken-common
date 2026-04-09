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
import org.taktik.icure.CardinalMetadataProperty
import org.taktik.icure.entities.RawJson
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DataAttachmentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DeletedAttachmentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DocumentLocationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DocumentStatusDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DocumentTypeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableRootDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents a Document. It is serialized in JSON and saved in the underlying CouchDB database.""",
)
/**
 * Represents a document entity stored in CouchDB. Documents can have main and secondary data attachments,
 * and support various storage backends (CouchDB attachments, object storage).
 */
data class DocumentDto(
	/** The Id of the document. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "The Id of the document. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@param:Schema(
		description = "The revision of the document in the database, used for conflict management / optimistic locking.",
	/** The revision of the document in the database, used for conflict management / optimistic locking. */
	) override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	override val modified: Long? = null,
	/** The id of the User that created this document. */
	override val author: String? = null,
	/** The id of the data owner that is responsible for this document. */
	override val responsible: String? = null,
	/** The medical location where this entity was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the document as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular document. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The location of the document. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "Location of the document") val documentLocation: DocumentLocationDto? = null,
	/** The type of document (e.g., admission, clinical path, document report, invoice). */
	@param:Schema(description = "The type of document, ex: admission, clinical path, document report,invoice, etc.") val documentType: DocumentTypeDto? = null,
	/** The status of the document development (e.g., Draft, finalized, reviewed, signed). */
	@param:Schema(description = "The status of the development of the document. Ex: Draft, finalized, reviewed, signed, etc.") val documentStatus: DocumentStatusDto? = null,
	/** When the document is stored externally, the URI of the document in that repository. */
	@param:Schema(description = "When the document is stored in an external repository, this is the uri of the document in that repository") val externalUri: String? = null,
	/** The name of the document. */
	@param:Schema(description = "Name of the document") val name: String? = null,
	/** The document version. */
	@param:Schema(description = "The document version") val version: String? = null,
	/** The ICureDocument (Form, Contact, ...) used to generate this document. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "The ICureDocument (Form, Contact, ...) that has been used to generate the document")
	val storedICureDocumentId: String? = null, // The ICureDocumentDto (FormDto, ContactDto, ...) that has been used to generate the document
	/** A unique external id from another external source. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "A unique external id (from another external source).") val externalUuid: String? = null,
	/** The size of the document file. */
	@param:Schema(description = "Size of the document file")
	@CardinalMetadataProperty
	val size: Long? = null,
	/** The hashed version of the document. */
	@param:Schema(description = "Hashed version of the document")
	@CardinalMetadataProperty
	val hash: String? = null,
	/** The id of the contact during which the document was created. */
	@param:Schema(description = "Id of the contact during which the document was created") val openingContactId: String? = null,
	/** The id of the main attachment stored as a CouchDB attachment. */
	@param:Schema(description = "Id of the main attachment of this document, if stored as a couchdb attachment")
	@CardinalMetadataProperty
	val attachmentId: String? = null,
	@param:Schema(
		description = "Id of the main attachment of this document, if stored using the object storage service",
	/** The id of the main attachment in the object storage service. */
	)
	@CardinalMetadataProperty
	val objectStoreReference: String? = null,
	@param:Schema(
		description = "The main Uniform Type Identifier for the main attachment (https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_conc/understand_utis_conc.html#//apple_ref/doc/uid/TP40001319-CH202-CHDHIJDE)",
	/** The main Uniform Type Identifier of the main attachment. */
	)
	@CardinalMetadataProperty
	val mainUti: String? = null,
	/** Extra Uniform Type Identifiers for the main attachment. */
	@param:Schema(description = "Extra Uniform Type Identifiers for the main attachment")
	@CardinalMetadataProperty
	val otherUtis: Set<String> = emptySet(),
	/** Secondary attachments for this document. */
	@param:Schema(description = "Secondary attachments for this document")
	@CardinalMetadataProperty
	val secondaryAttachments: Map<String, DataAttachmentDto> = emptyMap(),
	/** Information on past attachments for this document. */
	@param:Schema(description = "Information on past attachments for this document")
	@CardinalMetadataProperty
	val deletedAttachments: List<DeletedAttachmentDto> = emptyList(),
	/** The encrypted attachment content as bytes. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(type = "string", format = "byte")
	@CardinalMetadataProperty
	val encryptedAttachment: ByteArray? = null,
	/** The decrypted attachment content as bytes. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(type = "string", format = "byte")
	@CardinalMetadataProperty
	val decryptedAttachment: ByteArray? = null,
	/** The secret foreign keys, used for secure linking to patients. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The encrypted foreign keys. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The encryption keys used to encrypt secured properties, encrypted for separate Crypto Actors. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The base64-encoded encrypted fields of this document. */
	override val encryptedSelf: Base64StringDto? = null,
	/** The security metadata of this entity, for access control. */
	override val securityMetadata: SecurityMetadataDto? = null,
	override val extensions: RawJson.JsonObject? = null,
	override val extensionsVersion: Int? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEncryptionMetadataDto,
	EncryptableDto,
	ExtendableRootDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
