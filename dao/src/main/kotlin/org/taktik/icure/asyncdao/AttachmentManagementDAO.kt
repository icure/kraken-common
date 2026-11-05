/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.entity.AttachmentUpload
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.datastore.IDatastoreInformation
import java.nio.ByteBuffer

interface AttachmentManagementDAO<T : Identifiable<String>> : LookupDAO<T> {
	fun getAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String? = null): Flow<ByteBuffer>
	suspend fun createAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String, contentType: String, data: Flow<ByteBuffer>): String
	suspend fun deleteAttachment(datastoreInformation: IDatastoreInformation, documentId: String, rev: String, attachmentId: String): String

	/**
	 * Saves [entity] and writes [attachments] in a single request, so that the entity and its attachments are stored in
	 * the same revision: if the save fails, the entity keeps its revision.
	 *
	 * The `_attachments` of [entity] decide what happens to the attachments already stored: a stub keeps the stored
	 * attachment and an attachment with no entry is removed. An entry with the same name as one of [attachments] is
	 * replaced by the uploaded content.
	 */
	suspend fun saveWithAttachments(datastoreInformation: IDatastoreInformation, entity: T, attachments: Map<String, AttachmentUpload>): T
}
