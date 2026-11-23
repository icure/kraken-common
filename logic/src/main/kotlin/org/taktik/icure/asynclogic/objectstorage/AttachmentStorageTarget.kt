package org.taktik.icure.asynclogic.objectstorage

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.HasDataAttachments

/**
 * Where the attachments of an entity are stored: the datastore holding the entity (and its couchdb attachments) and
 * the object storage operations for that datastore.
 * Lets [DataAttachmentModificationLogic.updateAttachments] work on entities of a group other than the group of the
 * current user.
 */
interface AttachmentStorageTarget<T : HasDataAttachments<T>> {
	val datastoreInformation: IDatastoreInformation

	/**
	 * See [IcureObjectStorage.preStore]
	 */
	suspend fun preStore(entity: T, attachmentId: String, content: Flow<DataBuffer>, size: Long)

	/**
	 * See [IcureObjectStorage.scheduleStoreAttachment]
	 */
	suspend fun scheduleStoreAttachment(entity: T, attachmentId: String)

	/**
	 * See [IcureObjectStorage.scheduleDeleteAttachment]
	 */
	suspend fun scheduleDeleteAttachment(entity: T, attachmentId: String)

	companion object {
		/**
		 * The target for entities in [datastoreInformation], using the object storage of the group of the current user.
		 */
		fun <T : HasDataAttachments<T>> of(
			datastoreInformation: IDatastoreInformation,
			objectStorage: IcureObjectStorage<T>,
		): AttachmentStorageTarget<T> = object : AttachmentStorageTarget<T> {
			override val datastoreInformation: IDatastoreInformation = datastoreInformation

			override suspend fun preStore(entity: T, attachmentId: String, content: Flow<DataBuffer>, size: Long) =
				objectStorage.preStore(entity, attachmentId, content, size)

			override suspend fun scheduleStoreAttachment(entity: T, attachmentId: String) =
				objectStorage.scheduleStoreAttachment(entity, attachmentId)

			override suspend fun scheduleDeleteAttachment(entity: T, attachmentId: String) =
				objectStorage.scheduleDeleteAttachment(entity, attachmentId)
		}
	}
}
