package org.taktik.icure.asynclogic.objectstorage

/**
 * Name of groups of entities with data attachments, to allow grouping stored attachments by type of entity which owns the attachment.
 */
@Suppress("EnumEntryName")
enum class ObjectStorageEntityGroupName {
	receipts,
	documents
}