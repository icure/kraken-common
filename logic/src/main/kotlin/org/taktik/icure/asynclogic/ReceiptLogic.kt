/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.entities.Receipt
import org.taktik.icure.entities.embed.ReceiptBlobType
import java.nio.ByteBuffer

interface ReceiptLogic :
	EntityPersister<Receipt>,
	EntityWithSecureDelegationsLogic<Receipt>,
	ConflictResolutionLogic<Receipt>
{
	suspend fun createReceipt(receipt: Receipt): Receipt
	fun listReceiptsByReference(ref: String): Flow<Receipt>
	fun getAttachment(receiptId: String, attachmentId: String): Flow<ByteBuffer>

	/**
	 * Add receipt attachment in the old format, with only couchdb attachments supported
	 */
	suspend fun addReceiptAttachment(receipt: Receipt, blobType: ReceiptBlobType, payload: ByteArray): Receipt

	/**
	 * Add or replace receipt attachment in the new format. Attachment will be stored in couchdb, but may be moved
	 * later by an external process.
	 */
	suspend fun putReceiptAttachmentInfo(
		receiptId: String,
		receiptRev: String,
		blobType: ReceiptBlobType,
		compressionAlgorithm: String?,
		triedCompressionAlgorithmsVersion: String?,
		realDataSize: Long?,
		storedDataSize: Long,
		data: Flow<DataBuffer>,
	): Receipt

	fun getDataAttachmentByBlobType(receiptId: String, blobType: ReceiptBlobType): Flow<DataBuffer>
}
