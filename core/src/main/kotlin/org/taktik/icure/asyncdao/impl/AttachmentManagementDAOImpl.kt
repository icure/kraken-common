/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.taktik.icure.asyncdao.AttachmentManagementDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.StoredDocument
import java.nio.ByteBuffer

// TODO should use this one at some point instead of reimplementing the logic everywhere (on document dao, on receipt dao, ...)
abstract class AttachmentManagementDAOImpl<T : StoredDocument>(
	protected val entityClass: Class<T>,
	protected val couchDbDispatcher: CouchDbDispatcher,
	private val cacheChain: EntityCacheChainLink<String, T>?
) : AttachmentManagementDAO<T> {
	override fun getAttachment(
		datastoreInformation: IDatastoreInformation,
		documentId: String,
		attachmentId: String,
		rev: String?,
	): Flow<ByteBuffer> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getAttachment(documentId, attachmentId, rev))
	}

	override suspend fun createAttachment(
		datastoreInformation: IDatastoreInformation,
		documentId: String,
		attachmentId: String,
		rev: String,
		contentType: String,
		data: Flow<ByteBuffer>,
	): String {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return client.createAttachment(documentId, attachmentId, rev, contentType, data).also {
			cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(documentId))
		}
	}

	override suspend fun deleteAttachment(
		datastoreInformation: IDatastoreInformation,
		documentId: String,
		rev: String,
		attachmentId: String,
	): String {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return client.deleteAttachment(documentId, attachmentId, rev).also {
			cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(documentId))
		}
	}
}
