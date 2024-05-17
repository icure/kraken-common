/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import java.nio.ByteBuffer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.ReceiptDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.Receipt

@Repository("receiptDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type === 'org.taktik.icure.entities.Receipt' && !doc.deleted) emit(null, doc._id)}")
class ReceiptDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericIcureDAOImpl<Receipt>(Receipt::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), ReceiptDAO {

	@View(name = "by_reference", map = "classpath:js/receipt/By_ref.js")
	override fun listByReference(datastoreInformation: IDatastoreInformation, ref: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocs<String, String, Receipt>(createQuery(datastoreInformation, "by_reference").startKey(ref).endKey(ref + "\ufff0").includeDocs(true)).map { it.doc })
	}

	@View(name = "by_date", map = "function(doc) { if (doc.java_type === 'org.taktik.icure.entities.Receipt' && !doc.deleted) emit(doc.created)}")
	override fun listReceiptsAfterDate(datastoreInformation: IDatastoreInformation, date: Long) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocs<String, String, Receipt>(createQuery(datastoreInformation, "by_date").startKey(999999999999L).endKey(date).descending(true).includeDocs(true)).map { it.doc })
	}

	@View(name = "by_category", map = "function(doc) { if (doc.java_type === 'org.taktik.icure.entities.Receipt' && !doc.deleted) emit([doc.category,doc.subCategory,doc.created])}")
	override fun listReceiptsByCategory(datastoreInformation: IDatastoreInformation, category: String, subCategory: String, startDate: Long, endDate: Long) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocs<Array<String>, String, Receipt>(createQuery(datastoreInformation, "by_date").startKey(ComplexKey.of(category, subCategory, startDate)).endKey(ComplexKey.of(category, subCategory, endDate)).descending(true).includeDocs(true)).map { it.doc })
	}

	@View(name = "by_doc_id", map = "function(doc) { if (doc.java_type === 'org.taktik.icure.entities.Receipt' && !doc.deleted) emit(doc.documentId)}")
	override fun listReceiptsByDocId(datastoreInformation: IDatastoreInformation, date: Long) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocs<String, String, Receipt>(createQuery(datastoreInformation, "by_date").startKey(999999999999L).endKey(date).descending(true).includeDocs(true)).map { it.doc })
	}

	override fun getAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String?): Flow<ByteBuffer> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getAttachment(documentId, attachmentId, rev))
	}

	override suspend fun createAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String, contentType: String, data: Flow<ByteBuffer>): String {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return client.createAttachment(documentId, attachmentId, rev, contentType, data)
	}

	override suspend fun deleteAttachment(datastoreInformation: IDatastoreInformation, documentId: String, rev: String, attachmentId: String): String {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return client.deleteAttachment(documentId, attachmentId, rev)
	}
}
