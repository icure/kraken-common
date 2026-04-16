/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.PayloadTooLargeException
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.asyncdao.ReceiptDAO
import org.taktik.icure.asynclogic.ConflictResolutionLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.ReceiptLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.asynclogic.objectstorage.ReceiptDataAttachmentLoader
import org.taktik.icure.asynclogic.objectstorage.contentFlowOfNullable
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.Receipt
import org.taktik.icure.entities.embed.ReceiptBlobType
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.objectstorage.DataAttachment
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.mergers.Merger
import org.taktik.icure.utils.enforceSize
import org.taktik.icure.utils.toByteArray
import org.taktik.icure.validation.aspect.Fixer
import java.nio.ByteBuffer

private const val HARD_LIMIT_ATTACHMENT = 4 * 1024 * 1024 // 4 MB

open class ReceiptLogicImpl(
	private val receiptDAO: ReceiptDAO,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
	merger: Merger<Receipt>,
	@param:Qualifier("receiptDataAttachmentLoader") private val attachmentLoader: ReceiptDataAttachmentLoader,
) : EntityWithEncryptionMetadataLogic<Receipt, ReceiptDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters),
	ConflictResolutionLogic<Receipt> by ConflictResolutionLogicImpl(receiptDAO, merger, datastoreInstanceProvider),
	ReceiptLogic {
	override suspend fun createReceipt(receipt: Receipt) = createEntity(receipt)

	override fun entityWithUpdatedSecurityMetadata(
		entity: Receipt,
		updatedMetadata: SecurityMetadata,
	): Receipt = entity.copy(securityMetadata = updatedMetadata)

	override fun listReceiptsByReference(ref: String): Flow<Receipt> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(receiptDAO.listByReference(datastoreInformation, ref))
	}

	override fun getAttachment(
		receiptId: String,
		attachmentId: String,
	): Flow<ByteBuffer> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(receiptDAO.getAttachment(datastoreInformation, receiptId, attachmentId))
	}

	override suspend fun addReceiptAttachment(
		receipt: Receipt,
		blobType: ReceiptBlobType,
		payload: ByteArray,
	): Receipt {
		// TODO add hard limit after some time since we implemented client-side compression
		val datastoreInformation = getInstanceAndGroup()
		val newAttachmentId = DigestUtils.sha256Hex(payload)
		val modifiedReceipt = modifyEntities(listOf(receipt.copy(attachmentIds = receipt.attachmentIds + (blobType to newAttachmentId)))).first()
		val contentType = "application/octet-stream"
		return modifiedReceipt.copy(
			rev =
			receiptDAO.createAttachment(
				datastoreInformation,
				modifiedReceipt.id,
				newAttachmentId,
				modifiedReceipt.rev ?: error("Invalid receipt : no rev"),
				contentType,
				flowOf(ByteBuffer.wrap(payload)),
			),
		)
	}

	override suspend fun putReceiptAttachmentInfo(
		receiptId: String,
		receiptRev: String,
		blobType: ReceiptBlobType,
		compressionAlgorithm: String?,
		triedCompressionAlgorithmsVersion: String?,
		realDataSize: Long?,
		storedDataSize: Long,
		data: Flow<DataBuffer>
	): Receipt {
		// TODO replace hard limit with actual streaming of data to couchdb (problem: we can't now attachment id beforehand)
		val datastoreInformation = getInstanceAndGroup()
		if (storedDataSize > HARD_LIMIT_ATTACHMENT) throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Attachment size exceeds limit of $HARD_LIMIT_ATTACHMENT bytes")
		val payload = data.enforceSize(storedDataSize).toByteArray(true)
		val newAttachmentId = DigestUtils.sha256Hex(payload)
		val currentReceipt = receiptDAO.get(datastoreInformation, receiptId) ?: throw NotFoundRequestException("Receipt with id $receiptId not found")
		if (currentReceipt.attachmentIds.containsKey(blobType) || currentReceipt.attachmentInfos.containsKey(blobType)) {
			throw IllegalArgumentException("Another attachment for blob type $blobType already exists in receipt $receiptId")
		}
		receiptDAO.createAttachment(
			datastoreInformation,
			receiptId,
			newAttachmentId,
			receiptRev,
			"application/octet-stream",
			flowOf(ByteBuffer.wrap(payload)),
		)
		val receipt = receiptDAO.get(datastoreInformation, receiptId) ?: throw NotFoundRequestException("Receipt with id $receiptId not found")
		val updatedAttachmentInfo = receipt.attachmentInfos + Pair(
			blobType,
			DataAttachment(
				couchDbAttachmentId = newAttachmentId,
				objectStoreAttachmentId = null,
				utis = emptyList(),
				compressionAlgorithm = compressionAlgorithm,
				triedCompressionAlgorithmsVersion = triedCompressionAlgorithmsVersion,
				storedDataSize = storedDataSize,
				realDataSize = realDataSize,
			)
		)
		return modifyEntity(receipt.copy(attachmentInfos = updatedAttachmentInfo))
	}

	override fun getDataAttachmentByBlobType(
		receiptId: String,
		blobType: ReceiptBlobType
	): Flow<DataBuffer> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val receipt = receiptDAO.get(datastoreInformation, receiptId) ?: throw NotFoundRequestException("Receipt with id $receiptId not found")
		emitAll(
			receipt.attachmentIds[blobType]?.let { attachmentId ->
				getAttachment(receiptId, attachmentId)
			}?.map { DefaultDataBufferFactory.sharedInstance.wrap(it) }
				?: attachmentLoader.contentFlowOfNullable(receipt) { attachmentInfos[blobType] }
				?: throw NotFoundRequestException("No attachment for blob type $blobType in receipt $receiptId")
		)
	}

	override fun getGenericDAO(): ReceiptDAO = receiptDAO
}
