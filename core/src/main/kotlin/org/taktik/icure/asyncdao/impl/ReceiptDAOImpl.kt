/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

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
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.ReceiptDAO
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.Receipt
import org.taktik.icure.utils.main
import java.nio.ByteBuffer

@Repository("receiptDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type === 'org.taktik.icure.entities.Receipt' && !doc.deleted) emit(null, doc._id)}")
class ReceiptDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider,
) : ConflictDAOImpl<Receipt>(
	entityClass = Receipt::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider
), ReceiptDAO {

	@View(name = "by_reference", map = "classpath:js/receipt/By_ref.js")
	override fun listByReference(datastoreInformation: IDatastoreInformation, ref: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val query = createQuery(
			client = client,
			legacyView = "by_reference".main(),
			configurationView = "by_ref"
		).startKey(ref).endKey(ref + "\ufff0").includeDocs(true)
		emitAll(
			client.queryViewIncludeDocs<String, String, Receipt>(query).map { it.doc }
		)
	}

	@View(name = "by_date", map = "function(doc) { if (doc.java_type === 'org.taktik.icure.entities.Receipt' && !doc.deleted) emit(doc.created)}")
	override fun listReceiptsAfterDate(datastoreInformation: IDatastoreInformation, date: Long) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val query = createQuery(
			client = client,
			legacyView = "by_date".main(),
			configurationView = "by_date"
		).startKey(999999999999L).endKey(date).descending(true).includeDocs(true)
		emitAll(
			client.queryViewIncludeDocs<String, String, Receipt>(query).map { it.doc }
		)
	}

	@View(name = "by_category", map = "function(doc) { if (doc.java_type === 'org.taktik.icure.entities.Receipt' && !doc.deleted) emit([doc.category,doc.subCategory,doc.created])}")
	override fun listReceiptsByCategory(datastoreInformation: IDatastoreInformation, category: String, subCategory: String, startDate: Long, endDate: Long) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val query = createQuery(
			client = client,
			legacyView = "by_category".main(),
			configurationView = "by_category"
		).startKey(ComplexKey.of(category, subCategory, startDate))
			.endKey(ComplexKey.of(category, subCategory, endDate))
			.descending(true)
			.includeDocs(true)
		emitAll(
			client.queryViewIncludeDocs<Array<String>, String, Receipt>(query).map { it.doc }
		)
	}

	@View(name = "by_doc_id", map = "function(doc) { if (doc.java_type === 'org.taktik.icure.entities.Receipt' && !doc.deleted) emit(doc.documentId)}")
	override fun listReceiptsByDocId(datastoreInformation: IDatastoreInformation, date: Long) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val query = createQuery(
			client = client,
			legacyView = "by_doc_id".main(),
			configurationView = "by_doc_id"
		).startKey(999999999999L).endKey(date).descending(true).includeDocs(true)
		emitAll(
			client.queryViewIncludeDocs<String, String, Receipt>(query).map { it.doc }
		)
	}

	override fun getAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String?): Flow<ByteBuffer> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getAttachment(documentId, attachmentId, rev))
	}

	override suspend fun createAttachment(datastoreInformation: IDatastoreInformation, documentId: String, attachmentId: String, rev: String, contentType: String, data: Flow<ByteBuffer>): String {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return client.createAttachment(documentId, attachmentId, rev, contentType, data).also {
			cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(documentId))
		}
	}

	override suspend fun deleteAttachment(datastoreInformation: IDatastoreInformation, documentId: String, rev: String, attachmentId: String): String {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return client.deleteAttachment(documentId, attachmentId, rev).also {
			cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(documentId))
		}
	}

	@View(
		name = "conflicts",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Receipt' && !doc.deleted && doc._conflicts) emit(doc._id) }",
		secondaryPartition = MAURICE_PARTITION
	)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) =
		doListConflicts<Receipt>(datastoreInformation, "conflicts", MAURICE_PARTITION)

	override fun listIdsOfEntitiesWithConflicts(datastoreInformation: IDatastoreInformation): Flow<String> =
		doListIdsOfEntitiesWithConflicts<Receipt>(datastoreInformation, "conflicts", MAURICE_PARTITION)
}
