/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.domain.BatchUpdateDocumentInfo
import org.taktik.icure.entities.Document
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.exceptions.objectstorage.ObjectStorageException
import org.taktik.icure.pagination.PaginatedElement
import java.nio.ByteBuffer

interface DocumentService : EntityWithSecureDelegationsService<Document> {
	/**
	 * Creates a new document.
	 * It is generally not allowed to specify information related to attachments on creation (throws
	 * [IllegalArgumentException]), except for information related to the main attachment if [strict]
	 * is set to false (for retro-compatibility).
	 * It is never allowed to specify a non-empty value for deleted attachments.
	 * @param document the document to create
	 * @param strict specifies whether to behave in a strict or lenient way for the main attachment.
	 */
	suspend fun createDocument(
		document: Document,
		strict: Boolean = false
	): Document?

	suspend fun getMainAttachment(documentId: String): Flow<DataBuffer>

	suspend fun getDocument(documentId: String): Document?

	fun getAttachment(documentId: String, attachmentId: String): Flow<ByteBuffer>

	/**
	 * Modifies a document ensuring there is no change to deleted attachments and ids of attachments.
	 * If the updatedDocument changes the information on deleted attachments, or ids of attachments
	 * (including deletion of existing attachments or addition of new attachments) the method will
	 * fail with an [IllegalArgumentException]. The only exception to this is the main attachment (for
	 * retro-compatibility): in case there is an attempt to modify the main attachment ids, and [strict]
	 * is set to false the change will simply be ignored.
	 * This method still allows updating non-id attachment information such as utis.
	 * It is never allowed to modify deleted attachments.
	 * @param updatedDocument the new version of the document
	 * @param currentDocument the current document if already available, else null
	 * @param strict specifies whether to behave in a strict or lenient way for the main attachment.
	 * @return the updated document.
	 */
	suspend fun modifyDocument(updatedDocument: Document, currentDocument: Document, strict: Boolean = false): Document

	/**
	 * Create or modify multiple documents at once.
	 * This method can be executed both in a strict or lenient way. The strict and lenient behaviours are equivalent
	 * to [createDocument] for documents which will be newly created or to [modifyDocument] for documents which will be
	 * updated.
	 * If running in strict mode all documents will be checked before performing any modification, therefore if this throws
	 * [IllegalArgumentException] due to invalid document values no change has been performed to the stored data.
	 * @param documents information on documents to create / modify.
	 * @param strict specifies whether to behave in a strict or lenient way.
	 * @return the updated documents.
	 */
	fun createOrModifyDocuments(
		documents: List<BatchUpdateDocumentInfo>,
		strict: Boolean = false
	): Flow<Document>

	/**
	 * Updates the attachments for a document. For additional details check [DataAttachmentChange].
	 * @param currentDocument the document to update
	 * @param mainAttachmentChange specifies how to change the main attachment. If null the main attachment will be unchanged.
	 * @param secondaryAttachmentsChanges specifies how to change the secondary attachments. Only secondary attachments specified
	 * in this map will be changed, other attachments in the document will be ignored.
	 * @return the updated document.
	 * @throws [ObjectStorageException] if one or more attachments must be stored using the object
	 * storage service but this is not possible at the moment.
	 */
	suspend fun updateAttachments(
		currentDocument: Document,
		mainAttachmentChange: DataAttachmentChange? = null,
		secondaryAttachmentsChanges: Map<String, DataAttachmentChange> = emptyMap()
	): Document?

	fun listDocumentsByDocumentTypeHCPartySecretMessageKeys(documentTypeCode: String, hcPartyId: String, secretForeignKeys: List<String>): Flow<Document>

	/**
	 * Retrieves all the [Document]s for a given healthcare party id and a set of secret foreign keys, filtering out
	 * all the entities that the current user is not allowed to access.
	 * Note: if the current user is a data owner which data owner id is [hcPartyId], then all the search keys available
	 * for the user will be considered in retrieving the documents.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretForeignKeys a [List] of secret foreign keys.
	 * @return a [Flow] of [Document]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition requirement to query [Document]s.
	 */
	fun listDocumentsByHCPartySecretMessageKeys(hcPartyId: String, secretForeignKeys: List<String>): Flow<Document>

	/**
	 * Retrieves all the [Document]s for the given healthcare party id and secret foreign key in a format for pagination,
	 * filtering out all the entities that the current user cannot access.
	 * Note: differently from [listDocumentsByHCPartySecretMessageKeys], this method will NOT consider the available
	 * search keys for the current user if their data owner id is equal to [hcPartyId].
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretForeignKey the patient secret foreign key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginatedElement] wrapping the [Document]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition requirement to query [Document]s.
	 */
	fun listDocumentsByHcPartyIdAndSecretMessageKey(hcPartyId: String, secretForeignKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginatedElement>
	fun listDocumentsWithoutDelegation(limit: Int): Flow<Document>
	fun getDocuments(documentIds: List<String>): Flow<Document>

	fun solveConflicts(limit: Int? = null, ids: List<String>? = null): Flow<IdAndRev>
	suspend fun getDocumentsByExternalUuid(documentId: String): List<Document>

	/**
	 * Deletes a batch of [Document]s.
	 * If the user does not have the permission to delete an [Document] or the [Document] does not exist, then it
	 * is ignored without any warning.
	 *
	 * @param identifiers a [List] containing the ids of the [Document]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [Document]s successfully deleted.
	 */
	fun deleteDocuments(identifiers: Collection<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [Document].
	 *
	 * @param id the id of the [Document] to delete.
	 * @return a [DocIdentifier] related to the [Document] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [Document].
	 * @throws [NotFoundRequestException] if an [Document] with the specified [id] does not exist.
	 */
	suspend fun deleteDocument(id: String): DocIdentifier

	/**
	 * Modifies [Document]s in batch ensuring there is no change to deleted attachments and ids of attachments.
	 * If any of the documents changes the information on deleted attachments, or ids of attachments
	 * (including deletion of existing attachments or addition of new attachments) the method will
	 * ignore that modification.
	 * This method still allows updating non-id attachment information such as utis.
	 * It is never allowed to modify deleted attachments.
	 * @param documents a [Collection] of updated [Document]s.
	 * @return a [Flow] containing all the successfully updated [Document]s.
	 */
	suspend fun modifyDocuments(documents: Collection<Document>): Flow<Document>
}
