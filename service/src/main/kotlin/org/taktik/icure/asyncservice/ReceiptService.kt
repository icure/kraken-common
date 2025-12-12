/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.entities.Receipt
import org.taktik.icure.entities.embed.ReceiptBlobType
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import java.nio.ByteBuffer

interface ReceiptService : EntityWithSecureDelegationsService<Receipt> {
	suspend fun createReceipt(receipt: Receipt): Receipt
	suspend fun modifyReceipt(receipt: Receipt): Receipt
	fun listReceiptsByReference(ref: String): Flow<Receipt>
	fun getAttachment(receiptId: String, attachmentId: String): Flow<ByteBuffer>

	suspend fun addReceiptAttachment(receipt: Receipt, blobType: ReceiptBlobType, payload: ByteArray): Receipt

	/**
	 * Creates a batch of [Receipt]s.
	 *
	 * @param receipts a [Collection] of [Receipt]s to create.
	 * @return a [Flow] containing the created [Receipt]s.
	 * @throws [AccessDeniedException] if the user does not have the permissions to create [Receipt]s.
	 */
	fun createReceipts(receipts: Collection<Receipt>): Flow<Receipt>

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [Receipt]s.
	 */
	fun deleteReceipts(ids: List<IdAndRev>): Flow<Receipt>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the latest rev of the entity to delete.
	 * @return the deleted [Receipt].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteReceipt(id: String, rev: String?): Receipt

	/**
	 * Deletes an entity.
	 * An entity deleted this way can't be restored.
	 * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
	 *
	 * @param id the id of the entity
	 * @param rev the latest known revision of the entity.
	 * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun purgeReceipt(id: String, rev: String): DocIdentifier

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity
	 * @param id the id of the entity marked to restore
	 * @param rev the revision of the entity after it was marked as deleted
	 * @return the restored entity
	 */
	suspend fun undeleteReceipt(id: String, rev: String): Receipt

	/**
	 * Retrieve a [Receipt] by id.
	 *
	 * @param id the id of the [Receipt] to retrieve
	 * @return the [Receipt] or null, if it does not exist.
	 * @throws [AccessDeniedException] if the user does not meet the preconditions to access [Receipt]s or if
	 * it does not have the permissions to access that [Receipt]
	 */
	suspend fun getReceipt(id: String): Receipt?
}
