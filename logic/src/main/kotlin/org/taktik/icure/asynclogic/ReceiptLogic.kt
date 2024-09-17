/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import java.nio.ByteBuffer
import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.entities.Receipt
import org.taktik.icure.entities.embed.ReceiptBlobType

interface ReceiptLogic : EntityPersister<Receipt>, EntityWithSecureDelegationsLogic<Receipt> {
	suspend fun createReceipt(receipt: Receipt): Receipt?
	fun listReceiptsByReference(ref: String): Flow<Receipt>
	fun getAttachment(receiptId: String, attachmentId: String): Flow<ByteBuffer>
	suspend fun addReceiptAttachment(receipt: Receipt, blobType: ReceiptBlobType, payload: ByteArray): Receipt
}
