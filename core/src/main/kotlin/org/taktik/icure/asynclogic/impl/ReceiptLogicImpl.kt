/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.*
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ReceiptDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.ReceiptLogic
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.Receipt
import org.taktik.icure.entities.embed.ReceiptBlobType
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.validation.aspect.Fixer
import java.nio.ByteBuffer

@Service
@Profile("app")
class ReceiptLogicImpl(
	private val receiptDAO: ReceiptDAO,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters
) : EntityWithEncryptionMetadataLogic<Receipt, ReceiptDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters), ReceiptLogic {

	override suspend fun createReceipt(receipt: Receipt): Receipt? {
		if(receipt.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		return this.createEntities(listOf(receipt)).firstOrNull()
	}

	override fun entityWithUpdatedSecurityMetadata(entity: Receipt, updatedMetadata: SecurityMetadata): Receipt {
		return entity.copy(securityMetadata = updatedMetadata)
	}

	override suspend fun getEntity(id: String): Receipt? {
		val datastoreInformation = getInstanceAndGroup()
		return receiptDAO.get(datastoreInformation, id)
	}

	override fun listReceiptsByReference(ref: String): Flow<Receipt> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(receiptDAO.listByReference(datastoreInformation, ref))
	}

	override fun getAttachment(receiptId: String, attachmentId: String): Flow<ByteBuffer> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(receiptDAO.getAttachment(datastoreInformation, receiptId, attachmentId))
	}

	override suspend fun addReceiptAttachment(receipt: Receipt, blobType: ReceiptBlobType, payload: ByteArray): Receipt {
		val datastoreInformation = getInstanceAndGroup()
		val newAttachmentId = DigestUtils.sha256Hex(payload)
		val modifiedReceipt = modifyEntities(listOf(receipt.copy(attachmentIds = receipt.attachmentIds + (blobType to newAttachmentId)))).first()
		val contentType = "application/octet-stream"
		return modifiedReceipt.copy(rev = receiptDAO.createAttachment(datastoreInformation, modifiedReceipt.id, newAttachmentId, modifiedReceipt.rev ?: error("Invalid receipt : no rev"), contentType, flowOf(ByteBuffer.wrap(payload))))
	}

	override fun getGenericDAO(): ReceiptDAO = receiptDAO

}
