package org.taktik.icure.asynclogic.objectstorage

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.Receipt
import org.taktik.icure.entities.base.HasDataAttachments

/**
 * Handles the migration of data attachments.
 */
interface IcureObjectStorageMigration<T : HasDataAttachments<T>> {

	/**
	 * @param entity entity which owns the attachment.
	 * @param attachmentId id of the attachment.
	 * @return if there is a migration task scheduled for the provided entity and attachment.
	 */
	fun isMigrating(entity: T, attachmentId: String): Boolean

	/**
	 * Store an attachment previously stored as a couchdb attachment to the object storage service and schedules a migration task to be executed later.
	 * This method only schedules the tasks for execution, and may return before the tasks are actually completed.
	 * The migration task will remove the attachment from couchDb, only keeping a reference to the cloud-stored attachment.
	 * This task will only be executed if:
	 * - The document still refers to the same attachment. If the attachment changed the task will be completely removed.
	 * - The attachment has been successfully uploaded to the cloud. If the attachment was not updated the task will be delayed further.
	 * @param entity entity which owns the attachment.
	 * @param attachmentId id of the attachment.
	 */
	suspend fun scheduleMigrateAttachment(entity: T, attachmentId: String)

	/**
	 * Reschedules all migration tasks which could not be completed before the system was last shut down.
	 * This method only schedules the tasks for execution, and may return before the tasks are actually completed.
	 */
	suspend fun rescheduleStoredMigrationTasks()
}

private class NoObjectStorageMigration<T : HasDataAttachments<T>> : IcureObjectStorageMigration<T> {
	override fun isMigrating(entity: T, attachmentId: String): Boolean = false

	override suspend fun scheduleMigrateAttachment(entity: T, attachmentId: String) {
		throw IllegalStateException("Should not schedule migration for entity of type ${entity::class.simpleName}")
	}

	override suspend fun rescheduleStoredMigrationTasks() {
		// Do nothing
	}
}

interface DocumentObjectStorageMigration : IcureObjectStorageMigration<Document>
interface ReceiptObjectStorageMigration : IcureObjectStorageMigration<Receipt>
@Service
@Profile("app")
class ReceiptObjectStorageMigrationImpl :
	ReceiptObjectStorageMigration,
	IcureObjectStorageMigration<Receipt> by NoObjectStorageMigration()
