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
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ReceiptBlobTypeDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a receipt for a healthcare transaction. Receipts are used to store acknowledgements and responses
 * from external systems such as eFact, eAttest, or other healthcare messaging platforms. They can be linked
 * to invoices or other documents.
 */
data class ReceiptDto(
	/** The unique identifier of the receipt. */
	override val id: String,
	/** The revision of the receipt in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the User that created this receipt. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this receipt. */
	override val responsible: String? = null,
	/** The id of the medical location where this receipt was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the receipt as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular receipt. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** Map of blob type to attachment id for the receipt. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val attachmentIds: Map<ReceiptBlobTypeDto, String> = emptyMap(),
	/** List of references (e.g., nipReference, errorCode, errorPath, tarification, invoice UUID). */
	val references: List<String> = emptyList(), // nipReference:027263GFF152, errorCode:186, errorPath:/request/transaction, org.taktik.icure.services.external.rest.v2.dto;tarification:id, org.taktik.entities.InvoiceDto:UUID
	// The ICureDocumentDto (InvoiceDto, ContactDto, ...) this document is linked to
	/** The id of the document (InvoiceDto, ContactDto, ...) this receipt is linked to. */
	val documentId: String? = null,
	/** The category of the receipt. */
	val category: String? = null,
	/** The sub-category of the receipt. */
	val subCategory: String? = null,
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
