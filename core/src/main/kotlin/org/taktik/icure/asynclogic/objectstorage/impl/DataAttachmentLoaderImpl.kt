package org.taktik.icure.asynclogic.objectstorage.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.taktik.couchdb.exception.CouchDbException
import org.taktik.icure.asyncdao.AttachmentManagementDAO
import org.taktik.icure.asyncdao.DocumentDAO
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentLoader
import org.taktik.icure.asynclogic.objectstorage.DocumentDataAttachmentLoader
import org.taktik.icure.asynclogic.objectstorage.DocumentObjectStorage
import org.taktik.icure.asynclogic.objectstorage.DocumentObjectStorageMigration
import org.taktik.icure.asynclogic.objectstorage.IcureObjectStorage
import org.taktik.icure.asynclogic.objectstorage.IcureObjectStorageMigration
import org.taktik.icure.asynclogic.objectstorage.contentBytesOfNullable
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.base.HasDataAttachments
import org.taktik.icure.entities.objectstorage.DataAttachment
import org.taktik.icure.exceptions.IllegalEntityException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.properties.ObjectStorageProperties
import org.taktik.icure.security.CryptoUtils
import org.taktik.icure.security.CryptoUtils.isValidAesKey
import org.taktik.icure.security.CryptoUtils.keyFromHexString
import org.taktik.icure.utils.toByteArray
import java.security.GeneralSecurityException
import java.security.KeyException

class DataAttachmentLoaderImpl<T : HasDataAttachments<T>>(
	private val dao: AttachmentManagementDAO<T>,
	private val icureObjectStorage: IcureObjectStorage<T>,
	private val icureObjectStorageMigration: IcureObjectStorageMigration<T>,
	private val objectStorageProperties: ObjectStorageProperties,
	private val datastoreInstanceProvider: org.taktik.icure.datastore.DatastoreInstanceProvider,
) : DataAttachmentLoader<T> {
	private val migrationSizeLimit get() =
		objectStorageProperties.migrationSizeLimit.coerceAtLeast(objectStorageProperties.sizeLimit)

	suspend fun getInstanceAndGroup() = datastoreInstanceProvider.getInstanceAndGroup()

	override fun contentFlowOf(
		target: T,
		retrieveAttachment: T.() -> DataAttachment,
	): Flow<DataBuffer> = target.retrieveAttachment().let { attachment ->
		attachment.contentFlowFromCacheOrLoad(
			{ doLoadFlow(target, attachment) },
			{ flowOf(DefaultDataBufferFactory.sharedInstance.wrap(it)) },
		)
	}

	override suspend fun contentBytesOf(
		target: T,
		retrieveAttachment: T.() -> DataAttachment,
	): ByteArray = target.retrieveAttachment().let { attachment ->
		attachment.contentBytesFromCacheOrLoadAndStore { doLoadFlow(target, attachment).toByteArray(true) }
	}

	private fun doLoadFlow(
		target: T,
		attachment: DataAttachment,
	): Flow<DataBuffer> = attachment.objectStoreAttachmentId?.let {
		icureObjectStorage.readAttachment(target, it)
	} ?: attachment.couchDbAttachmentId!!.let { attachmentId ->
		if (icureObjectStorageMigration.isMigrating(target, attachmentId)) {
			icureObjectStorage.tryReadCachedAttachment(target, attachmentId) ?: loadCouchDbAttachment(target, attachmentId)
		} else {
			flow {
				if (shouldMigrate(target, attachmentId)) icureObjectStorageMigration.scheduleMigrateAttachment(target, attachmentId)
				emitAll(loadCouchDbAttachment(target, attachmentId))
			}
		}
	}

	private fun loadCouchDbAttachment(
		target: T,
		attachmentId: String,
	) = flow {
		try {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(dao.getAttachment(datastoreInformation, target.id, attachmentId))
		} catch (e: CouchDbException) {
			if (e.statusCode == 404) {
				throw IllegalEntityException("Attachment $attachmentId declared on ${target::class.simpleName} ${target.id} doesn't actually exist")
			} else {
				throw e
			}
		}
	}.map { DefaultDataBufferFactory.sharedInstance.wrap(it) }

	private fun shouldMigrate(
		target: T,
		attachmentId: String,
	) = objectStorageProperties.backlogToObjectStorage &&
		target.attachments
			?.get(attachmentId)
			?.length
			?.let { it >= migrationSizeLimit } == true
}

@Service("documentDataAttachmentLoader")
@Profile("app")
class DocumentDataAttachmentLoaderImpl(
	dao: DocumentDAO,
	objectStorage: DocumentObjectStorage,
	objectStorageMigration: DocumentObjectStorageMigration,
	objectStorageProperties: ObjectStorageProperties,
	datastoreInstanceProvider: org.taktik.icure.datastore.DatastoreInstanceProvider,
) : DocumentDataAttachmentLoader,
	DataAttachmentLoader<Document> by DataAttachmentLoaderImpl(
		dao,
		objectStorage,
		objectStorageMigration,
		objectStorageProperties,
		datastoreInstanceProvider,
	) {
	override suspend fun decryptAttachment(
		document: Document?,
		enckeys: String?,
		retrieveAttachment: Document.() -> DataAttachment?,
	): ByteArray? = decryptAttachment(document, if (enckeys.isNullOrBlank()) emptyList() else enckeys.split(','), retrieveAttachment)

	override suspend fun decryptAttachment(
		document: Document?,
		enckeys: List<String>,
		retrieveAttachment: Document.() -> DataAttachment?,
	): ByteArray? = contentBytesOfNullable(document, retrieveAttachment)?.let { content ->
		enckeys
			.asSequence()
			.filter { sfk -> sfk.keyFromHexString().isValidAesKey() }
			.mapNotNull { sfk ->
				try {
					CryptoUtils.decryptAES(content, sfk.keyFromHexString())
				} catch (_: GeneralSecurityException) {
					null
				} catch (_: KeyException) {
					null
				} catch (_: IllegalArgumentException) {
					null
				}
			}.firstOrNull()
			?: content
	}
}
