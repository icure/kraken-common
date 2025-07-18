package org.taktik.icure.asynclogic.objectstorage.impl

import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.asyncdao.AttachmentManagementDAO
import org.taktik.icure.asyncdao.DocumentDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.objectstorage.DocumentDataAttachmentModificationLogic
import org.taktik.icure.asynclogic.objectstorage.DocumentObjectStorage
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentModificationLogic
import org.taktik.icure.asynclogic.objectstorage.IcureObjectStorage
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.base.HasDataAttachments
import org.taktik.icure.entities.embed.DeletedAttachment
import org.taktik.icure.entities.objectstorage.DataAttachment
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.properties.ObjectStorageProperties
import org.taktik.icure.utils.toByteArray

abstract class DataAttachmentModificationLogicImpl<T : HasDataAttachments<T>>(
	private val dao: AttachmentManagementDAO<T>,
	private val icureObjectStorage: IcureObjectStorage<T>,
	private val objectStorageProperties: ObjectStorageProperties,
	private val datastoreInstanceProvider: org.taktik.icure.datastore.DatastoreInstanceProvider
) : DataAttachmentModificationLogic<T> {
	suspend fun getInstanceAndGroup() = datastoreInstanceProvider.getInstanceAndGroup()

	override fun ensureValidAttachmentChanges(currEntity: T, newEntity: T, lenientKeys: Set<String>): T {
		check(currEntity.attachments == newEntity.attachments) {
			"Couchdb attachments for new entity should have been updated to match current entity."
		}
		val currentAttachments = currEntity.dataAttachments
		val newAttachments = newEntity.dataAttachments
		return newEntity.withDataAttachments(
			(currentAttachments.keys + newAttachments.keys).mapNotNull { key ->
				ensureNoContentChange(currentAttachments[key], newAttachments[key], key !in lenientKeys, key)
					?.let { key to it }
			}.toMap()
		).also {
			require(it.deletedAttachments == currEntity.deletedAttachments) {
				"Deleted attachments information can't be changed manually."
			}
		}
	}

	private fun ensureNoContentChange(
		currentAttachment: DataAttachment?,
		updatedAttachment: DataAttachment?,
		strict: Boolean,
		attachmentKey: String
	): DataAttachment? =
		if (currentAttachment != null) {
			if (updatedAttachment != null && updatedAttachment hasSameIdsAs currentAttachment) {
				updatedAttachment
			} else {
				require(!strict) {
					"Inconsistency between updated and current for attachment $attachmentKey: " +
						"expected ${currentAttachment.ids} but got ${updatedAttachment?.ids}"
				}
				updatedAttachment?.withIdsOf(currentAttachment) ?: currentAttachment
			}
		} else {
			require(!strict || updatedAttachment == null) {
				"Attachment $attachmentKey is in updated document but does not exist in current document"
			}
			null
		}

	override suspend fun updateAttachments(
		entityId: String,
		expectedRev: String?,
		changes: Map<String, DataAttachmentChange>
	): T? {
		// 1. Validate all attachments changes make sense:
		// - The user is not asking to delete an attachment that doesn't exist
		// - The user is not asking to create an attachment with the same content as an existing attachment (can only be checked for couchdb attachments but maybe in future also for object storage)
		// - No update requests with duplicate attachments data (can only be checked for couchdb attachments but maybe in future also for object storage)
		val datastoreInformation = getInstanceAndGroup()
		val currEntity = dao.get(datastoreInformation, entityId) ?: throw NotFoundRequestException("Couldn't find entity with id $entityId")
		if (expectedRev != null && currEntity.rev != expectedRev) throw ConflictRequestException("Rev mismatch for $entityId: expected $expectedRev")
		validateDeleteRequests(currEntity, changes)
		val dataAttachmentUpdateInfos = changes.mapValues { (key, change) ->
			prepareTaskForChange(currEntity, currEntity.dataAttachments[key], change)
		}
		if (
			dataAttachmentUpdateInfos
				.mapNotNull { it.value.newAttachment?.couchDbAttachmentId }
				.groupingBy { it }
				.eachCount()
				.any { it.value > 1 }
		) throw IllegalArgumentException(
			"Different create or update attachment requests have the same attachment content"
		)
		val tasks = dataAttachmentUpdateInfos.flatMap { it.value.tasks }
		// 2. Pre-store object storage attachment and store couchdb attachments
		val updatedRev = createCouchdbAttachments(
			entityId = entityId,
			initialRev = currEntity.rev!!,
			tasks = tasks,
			datastoreInformation = datastoreInformation
		)
		preStoreObjectStorageAttachments(
			currEntity,
			tasks
		)
		/*
		 * 3. Update entity with:
		 * - Updated data attachment information
		 * - Deleted unused or obsolete couchdb attachments
		 */
		val updatedDataAttachmentsInfo = (currEntity.dataAttachments.keys + dataAttachmentUpdateInfos.keys).flatMap { key ->
			dataAttachmentUpdateInfos[key]?.let { updated ->
				listOfNotNull(updated.newAttachment?.let { key to it })
			} ?: listOf(key to currEntity.dataAttachments.getValue(key))
		}.toMap()
		val updatedDeletedAttachmentsInfo = System.currentTimeMillis().let { now ->
			currEntity.deletedAttachments + dataAttachmentUpdateInfos.mapNotNull { (key, result) ->
				result.deletedAttachment?.let {
					DeletedAttachment(
						it.couchDbAttachmentId,
						it.objectStoreAttachmentId,
						key,
						now
					)
				}
			}
		}
		val newAttachments = updatedDataAttachmentsInfo.values.mapNotNull { it.couchDbAttachmentId }.associateWith {
			Attachment(isStub = true)
		}
		return dao.save(
			datastoreInformation,
			currEntity.updateAttachmentInformation(
				rev = updatedRev,
				attachments = newAttachments,
				dataAttachments = updatedDataAttachmentsInfo,
				deletedAttachments = updatedDeletedAttachmentsInfo
			)
		)?.also {
			// 4. Schedule upload for new object storage attachment and schedule deletion for obsolete object storage attachments
			scheduleObjectStorageTasks(it, tasks)
		}
	}

	private fun validateDeleteRequests(currEntity: T, changes: Map<String, DataAttachmentChange>) {
		changes.asSequence().filter { it.value.isDelete }.map { it.key }.forEach {
			require(it in currEntity.dataAttachments) { "Can't delete attachment with key $it: no attachment whith such key" }
		}
	}

	private suspend fun prepareTaskForChange(
		entity: T,
		currentAttachment: DataAttachment?,
		change: DataAttachmentChange
	): AttachmentTaskDetails = when (change) {
		is DataAttachmentChange.CreateOrUpdate -> prepareCreateOrUpdateAttachmentTask(entity, currentAttachment, change)
		DataAttachmentChange.Delete -> prepareDeleteAttachmentTask(currentAttachment)
	}

	private suspend fun prepareCreateOrUpdateAttachmentTask(
		entity: T,
		currentAttachment: DataAttachment?,
		change: DataAttachmentChange.CreateOrUpdate
	): AttachmentTaskDetails {
		val (newAttachment, uploadTask) = doPreStoreAndPrepareAttachmentInfoAndTasks(
			entity,
			change,
			change.utis ?: currentAttachment?.utis ?: emptyList()
		)
		return AttachmentTaskDetails(
			newAttachment = newAttachment,
			deletedAttachment = currentAttachment,
			tasks = makeDeleteTasksFor(currentAttachment) + listOfNotNull(uploadTask)
		)
	}

	private suspend fun doPreStoreAndPrepareAttachmentInfoAndTasks(
		entity: T,
		change: DataAttachmentChange.CreateOrUpdate,
		utis: List<String>
	): Pair<DataAttachment, AttachmentTask?> =
		if (change.size < objectStorageProperties.sizeLimit) {
			val bytes = change.data.toByteArray(true)
			val attachmentId = DigestUtils.sha256Hex(bytes)
			// Will fail also in case of conflicting delete and update requests (delete request would affect the attachment created by this request)
			if (entity.dataAttachments.values.any { it.couchDbAttachmentId == attachmentId }) throw IllegalArgumentException(
				"Duplicate attachment content for ${entity::class.java.simpleName} ${entity.id}: $attachmentId"
			)
			DataAttachment(couchDbAttachmentId = attachmentId, objectStoreAttachmentId = null, utis = utis).let { dataAttachment ->
				Pair(
					dataAttachment,
					if (entity.attachments?.keys?.contains(attachmentId) == true)
						// Attachment was successfully uploaded in a previous request but we failed to fully update the entity data attachment metadata
						null
					else
						AttachmentTask.UploadCouchDb(
							attachmentId,
							bytes,
							if (change.dataIsEncrypted) "application/octet-stream" else dataAttachment.mimeTypeOrDefault
						)
				)
			}
		} else {
			// Object storage attachment could be massive -> too expensive to decide the id based on sha
			// If previously there was a successful store of the attachment with failed entity data attachment metadata update
			// we will duplicate the attachment on object storage, but it can be cleaned up by the periodic cleanup task.
			val attachmentId = UUID.randomUUID().toString()
			Pair(
				DataAttachment(couchDbAttachmentId = null, objectStoreAttachmentId = attachmentId, utis = utis),
				AttachmentTask.PreStoreAndUploadObjectStorage(
					attachmentId,
					change.data,
					change.size
				)
			)
		}

	private fun prepareDeleteAttachmentTask(attachment: DataAttachment?) = AttachmentTaskDetails(
		newAttachment = null,
		deletedAttachment = attachment,
		tasks = makeDeleteTasksFor(attachment)
	)

	private fun makeDeleteTasksFor(attachment: DataAttachment?) = attachment?.let {
		listOfNotNull(
			attachment.couchDbAttachmentId?.let { AttachmentTask.DeleteCouchDb(it) },
			attachment.objectStoreAttachmentId?.let { AttachmentTask.DeleteObjectStorage(it) }
		)
	} ?: emptyList()

	private suspend fun scheduleObjectStorageTasks(
		entity: T,
		tasks: List<AttachmentTask>,
	) {
		tasks.filterIsInstance<AttachmentTask.PreStoreAndUploadObjectStorage>().forEach { task ->
			icureObjectStorage.scheduleStoreAttachment(entity, task.attachmentId)
		}
		tasks.filterIsInstance<AttachmentTask.DeleteObjectStorage>().forEach { task ->
			icureObjectStorage.scheduleDeleteAttachment(entity, task.attachmentId)
		}
	}

	protected abstract fun T.updateAttachmentInformation(
		rev: String,
		attachments: Map<String, Attachment>?,
		dataAttachments: Map<String, DataAttachment>,
		deletedAttachments: List<DeletedAttachment>
	): T

	protected abstract fun T.updateRevision(rev: String): T

	private val DataAttachmentChange.isDelete get() = this === DataAttachmentChange.Delete

	private suspend fun createCouchdbAttachments(
		entityId: String,
		initialRev: String,
		tasks: List<AttachmentTask>,
		datastoreInformation: IDatastoreInformation
	): String =
		tasks.filterIsInstance<AttachmentTask.UploadCouchDb>().fold(initialRev) { latestRev, task ->
			dao.createAttachment(
				datastoreInformation,
				entityId,
				task.attachmentId,
				latestRev,
				task.mimeType,
				flowOf(ByteBuffer.wrap(task.data))
			)
		}

	private suspend fun preStoreObjectStorageAttachments(
		entity: T,
		tasks: List<AttachmentTask>,
	) {
		tasks.filterIsInstance<AttachmentTask.PreStoreAndUploadObjectStorage>().forEach {
			icureObjectStorage.preStore(
				entity,
				it.attachmentId,
				it.dataFlow,
				it.size
			)
		}
	}

	private data class AttachmentTaskDetails(
		val newAttachment: DataAttachment?,
		val deletedAttachment: DataAttachment?,
		val tasks: List<AttachmentTask> // Tasks still need to be performed
	)

	private sealed interface AttachmentTask {
		class UploadCouchDb(val attachmentId: String, val data: ByteArray, val mimeType: String) : AttachmentTask
		class PreStoreAndUploadObjectStorage(val attachmentId: String, val dataFlow: Flow<DataBuffer>, val size: Long) : AttachmentTask
		class DeleteCouchDb(val attachmentId: String) : AttachmentTask
		class DeleteObjectStorage(val attachmentId: String) : AttachmentTask
	}

	override suspend fun cleanupPurgedEntityAttachments(purgedEntity: T) {
		purgedEntity.dataAttachments.values.forEach { attachment ->
			attachment.objectStoreAttachmentId?.also {
				icureObjectStorage.scheduleDeleteAttachment(purgedEntity, it)
			}
		}
	}
}

@Service("documentDataAttachmentModificationLogic")
@Profile("app")
class DocumentDataAttachmentModificationLogicImpl(
	dao: DocumentDAO,
	icureObjectStorage: DocumentObjectStorage,
	objectStorageProperties: ObjectStorageProperties,
	datastoreInstanceProvider: org.taktik.icure.datastore.DatastoreInstanceProvider
): DocumentDataAttachmentModificationLogic, DataAttachmentModificationLogic<Document> by object : DataAttachmentModificationLogicImpl<Document>(
	dao,
	icureObjectStorage,
	objectStorageProperties,
	datastoreInstanceProvider
) {
	override fun Document.updateAttachmentInformation(
		rev: String,
		attachments: Map<String, Attachment>?,
		dataAttachments: Map<String, DataAttachment>,
		deletedAttachments: List<DeletedAttachment>
	): Document = this
		.copy(rev = rev, attachments = attachments, deletedAttachments = deletedAttachments)
		.withDataAttachments(dataAttachments)

	override fun Document.updateRevision(rev: String) = copy(rev = rev)
}
