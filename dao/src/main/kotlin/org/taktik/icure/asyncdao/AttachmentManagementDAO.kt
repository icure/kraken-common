/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import java.nio.ByteBuffer
import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.datastore.IDatastoreInformation

interface AttachmentManagementDAO<T : Identifiable<String>> : LookupDAO<T> {
	fun getAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String? = null): Flow<ByteBuffer>
	suspend fun createAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String, contentType: String, data: Flow<ByteBuffer>): String
	suspend fun deleteAttachment(datastoreInformation: IDatastoreInformation, documentId: String, rev: String, attachmentId: String): String
}
