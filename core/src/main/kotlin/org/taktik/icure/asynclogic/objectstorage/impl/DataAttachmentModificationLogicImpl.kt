package org.taktik.icure.asynclogic.objectstorage.impl

import java.nio.ByteBuffer
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.asyncdao.AttachmentManagementDAO
import org.taktik.icure.asyncdao.DocumentDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.objectstorage.DocumentDataAttachmentModificationLogic
import org.taktik.icure.asynclogic.objectstorage.DocumentObjectStorage
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentModificationLogic
import org.taktik.icure.asynclogic.objectstorage.IcureObjectStorage
import org.taktik.icure.asynclogic.objectstorage.TmpDocumentDataAttachmentModificationLogic
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.base.HasDataAttachments
import org.taktik.icure.entities.embed.DeletedAttachment
import org.taktik.icure.entities.objectstorage.DataAttachment
import org.taktik.icure.properties.ObjectStorageProperties
import org.taktik.icure.utils.toByteArray

abstract class DataAttachmentModificationLogicImpl<T : HasDataAttachments<T>>(
    private val dao: AttachmentManagementDAO<T>,
    private val icureObjectStorage: IcureObjectStorage<T>,
    private val objectStorageProperties: ObjectStorageProperties,
    private val datastoreInstanceProvider: org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
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

	override suspend fun updateAttachments(currEntity: T, changes: Map<String, DataAttachmentChange>): T? {
		// First pre-check all the deletions are valid: avoid pre-storing some elements then cancelling the update because of invalid deletion
		validateChanges(currEntity, changes)
		val updateResults = changes.mapValues { (key, change) ->
			applyChange(currEntity, currEntity.dataAttachments[key], change)
		}
		val tasks = updateResults.flatMap { it.value.tasks }
		val newAttachments = currEntity.attachments?.let {
			it - tasks.filterIsInstance<AttachmentTask.DeleteCouchDb>().map { it.attachmentId }.toSet()
		}
		val newDataAttachments = (currEntity.dataAttachments.keys + updateResults.keys).flatMap { key ->
			updateResults[key]?.let { updated ->
				listOfNotNull(updated.newAttachment?.let { key to it })
			} ?: listOf(key to currEntity.dataAttachments.getValue(key))
		}.toMap()
		val newDeletedAttachments = System.currentTimeMillis().let { now ->
			currEntity.deletedAttachments + updateResults.mapNotNull { (key, result) ->
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
		val datastoreInformation = getInstanceAndGroup()
		return dao.save(
			datastoreInformation,
			currEntity.updateAttachmentInformation(
				rev = performPreSaveAttachmentTasks(currEntity, tasks, datastoreInformation),
				attachments = newAttachments,
				dataAttachments = newDataAttachments,
				deletedAttachments = newDeletedAttachments
			)
		)?.let { savedDoc ->
			savedDoc.updateRevision(performPostSaveAttachmentTasks(savedDoc, tasks, datastoreInformation))
		}
	}

	private fun validateChanges(currEntity: T, changes: Map<String, DataAttachmentChange>) {
		changes.asSequence().filter { it.value.isDelete }.map { it.key }.forEach {
			require(it in currEntity.dataAttachments) { "Can't delete attachment with key $it: no attachment whith such key" }
		}
	}

	private suspend fun applyChange(
        entity: T,
        currentAttachment: DataAttachment?,
        change: DataAttachmentChange
	): AttachmentUpdateResult = when (change) {
		is DataAttachmentChange.CreateOrUpdate -> createOrUpdateAttachment(entity, currentAttachment, change)
		DataAttachmentChange.Delete -> deleteAttachment(currentAttachment)
	}

	private suspend fun createOrUpdateAttachment(
        entity: T,
        currentAttachment: DataAttachment?,
        change: DataAttachmentChange.CreateOrUpdate
	): AttachmentUpdateResult {
		val (newAttachment, uploadTask) = createAttachment(entity, change, change.utis ?: currentAttachment?.utis ?: emptyList())
		return AttachmentUpdateResult(
			newAttachment = newAttachment,
			deletedAttachment = currentAttachment,
			tasks = deleteTasksFor(currentAttachment) + uploadTask
		)
	}

	private suspend fun  createAttachment(
		entity: T,
		change: DataAttachmentChange.CreateOrUpdate,
		utis: List<String>
	): Pair<DataAttachment, AttachmentTask> =
		if (change.size >= objectStorageProperties.sizeLimit)
			createObjectStorageAttachment(entity, change, utis)
		else
			createCouchDbAttachment(change, utis)

	private suspend fun createObjectStorageAttachment(
		entity: T,
		change: DataAttachmentChange.CreateOrUpdate,
		utis: List<String>
	): Pair<DataAttachment, AttachmentTask> = UUID.randomUUID().toString().let { attachmentId ->
		icureObjectStorage.preStore(entity, attachmentId, change.data, change.size)
		Pair(
			DataAttachment(couchDbAttachmentId = null, objectStoreAttachmentId = attachmentId, utis = utis),
			AttachmentTask.UploadObjectStorage(attachmentId)
		)
	}

	private suspend fun createCouchDbAttachment(
		change: DataAttachmentChange.CreateOrUpdate,
		utis: List<String>
	): Pair<DataAttachment, AttachmentTask> {
		val bytes = change.data.toByteArray(true)
		val attachmentId = DigestUtils.sha256Hex(bytes)
		return DataAttachment(couchDbAttachmentId = attachmentId, objectStoreAttachmentId = null, utis = utis).let {
			it to AttachmentTask.UploadCouchDb(
				attachmentId,
				bytes,
				if (change.dataIsEncrypted) "application/octet-stream" else it.mimeTypeOrDefault
			)
		}
	}

	private fun deleteAttachment(attachment: DataAttachment?) = AttachmentUpdateResult(
		newAttachment = null,
		deletedAttachment = attachment,
		tasks = deleteTasksFor(attachment)
	)

	private fun deleteTasksFor(attachment: DataAttachment?) = attachment?.let {
		listOfNotNull(
			attachment.couchDbAttachmentId?.let { AttachmentTask.DeleteCouchDb(it) },
			attachment.objectStoreAttachmentId?.let { AttachmentTask.DeleteObjectStorage(it) }
		)
	} ?: emptyList()

	private suspend fun performPreSaveAttachmentTasks(
		entity: T,
		tasks: List<AttachmentTask>,
		datastoreInformation: IDatastoreInformation
	): String = tasks
		.filterIsInstance<AttachmentTask.DeleteCouchDb>()
		.fold(entity.rev!!) { latestRev, task ->
			entity.attachments?.takeIf { it.containsKey(task.attachmentId) }?.let {
				dao.deleteAttachment(datastoreInformation, entity.id, latestRev, task.attachmentId)
			} ?: latestRev
		}

	private suspend fun performPostSaveAttachmentTasks(
		entity: T,
		tasks: List<AttachmentTask>,
		datastoreInformation: IDatastoreInformation
	): String = tasks
		.filterNot { it is AttachmentTask.DeleteCouchDb }
		.fold(entity.rev!!) { latestRev, task ->
			when (task) {
				is AttachmentTask.DeleteObjectStorage ->
					latestRev.also { icureObjectStorage.scheduleDeleteAttachment(entity, task.attachmentId) }
				is AttachmentTask.UploadCouchDb ->
					dao.createAttachment(
						datastoreInformation,
						entity.id,
						task.attachmentId,
						latestRev,
						task.mimeType,
						flowOf(ByteBuffer.wrap(task.data))
					)
				is AttachmentTask.UploadObjectStorage ->
					latestRev.also { icureObjectStorage.scheduleStoreAttachment(entity, task.attachmentId) }
				is AttachmentTask.DeleteCouchDb ->
					throw IllegalStateException("DeleteCouchDb tasks should have been filtered out")
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

	private data class AttachmentUpdateResult(
        val newAttachment: DataAttachment?,
        val deletedAttachment: DataAttachment?,
        val tasks: List<AttachmentTask>
	)

	private sealed interface AttachmentTask {
		class UploadCouchDb(val attachmentId: String, val data: ByteArray, val mimeType: String) : AttachmentTask
		class UploadObjectStorage(val attachmentId: String) : AttachmentTask
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
    datastoreInstanceProvider: org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
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
