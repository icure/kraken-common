/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.DocumentDAO
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.Document
import org.taktik.icure.utils.distinctById
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.main
import java.nio.ByteBuffer

@Repository("documentDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Document' && !doc.deleted) emit( null, doc._id )}")
class DocumentDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<Document>(
	Document::class.java,
	couchDbDispatcher,
	idGenerator,
	entityCacheFactory.getConfiguredCache(),
	designDocumentProvider,
	daoConfig = daoConfig
), DocumentDAO {

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Document' && !doc.deleted && doc._conflicts) emit(doc._id )}")
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "conflicts")
			.limit(200)
			.includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<String, Document>(viewQuery).map { it.doc })
	}

	@Deprecated("This method cannot include results with secure delegations, use listDocumentIdsByDataOwnerPatientCreated instead")
	@Views(
    	View(name = "by_hcparty_message", map = "classpath:js/document/By_hcparty_message_map.js"),
		View(name = "by_data_owner_message", map = "classpath:js/document/By_data_owner_message_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listDocumentsByHcPartyAndSecretMessageKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretForeignKeys.flatMap { fk ->
			searchKeys.map { arrayOf(it, fk) }
		}
		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_message".main()
		).keys(keys).includeDocs()
		emitAll(client.interleave<Array<String>, String, Document>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, String, Document>>().map { it.doc })
	}.distinctById()

	@View(name = "by_hcparty_message_date", map = "classpath:js/document/By_hcparty_message_date_map.js", secondaryPartition = MAURICE_PARTITION)
	override fun listDocumentIdsByDataOwnerPatientCreated(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = getEntityIdsByDataOwnerPatientDate(
		views = listOf("by_hcparty_message_date" to MAURICE_PARTITION, "by_data_owner_message" to DATA_OWNER_PARTITION),
		datastoreInformation = datastoreInformation,
		searchKeys = searchKeys,
		secretForeignKeys = secretForeignKeys,
		startDate = startDate,
		endDate = endDate,
		descending = descending
	)

	@View(name = "without_delegations", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Document' && !doc.deleted && (!doc.delegations || Object.keys(doc.delegations).length === 0)) emit(doc._id )}")
	override fun listDocumentsWithNoDelegations(datastoreInformation: IDatastoreInformation, limit: Int) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "without_delegations")
			.limit(limit)
			.includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<String, Document>(viewQuery).map { it.doc })
	}

	@Views(
		View(name = "by_type_hcparty_message", map = "classpath:js/document/By_document_type_hcparty_message_map.js"),
		View(name = "by_type_data_owner_message", map = "classpath:js/document/By_document_type_data_owner_message_map.js", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listDocumentsByDocumentTypeHcPartySecretMessageKeys(datastoreInformation: IDatastoreInformation, documentTypeCode: String, searchKeys: Set<String>, secretForeignKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretForeignKeys.flatMap { fk ->
			searchKeys.map { ComplexKey.of(documentTypeCode, it, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_type_hcparty_message",
			"by_type_data_owner_message" to DATA_OWNER_PARTITION
		).keys(keys).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Document>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}, {it.components[2] as? String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Document>>().map { it.doc })
	}.distinctById()

	override fun listDocumentIdsByDocumentTypeHcPartySecretMessageKeys(
		datastoreInformation: IDatastoreInformation,
		documentTypeCode: String,
		searchKeys: Set<String>,
		secretForeignKeys: List<String>
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretForeignKeys.flatMap { fk ->
			searchKeys.map { ComplexKey.of(documentTypeCode, it, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_type_hcparty_message",
			"by_type_data_owner_message" to DATA_OWNER_PARTITION
		).keys(keys).doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy({it.components[0] as? String}, {it.components[1] as? String}, {it.components[2] as? String})
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id }
		)
	}

	@View(name = "by_externalUuid", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Document' && !doc.deleted && doc.externalUuid) emit( doc.externalUuid, doc._id )}")
	override suspend fun listDocumentsByExternalUuid(datastoreInformation: IDatastoreInformation, externalUuid: String): List<Document> {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_externalUuid")
			.key(externalUuid)
			.includeDocs(true)

		return client.queryViewIncludeDocs<String, String, Document>(viewQuery).map { it.doc /*postLoad(datastoreInformation, it.doc)*/ }.toList().sortedByDescending { it.created ?: 0 }
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

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_message" to DATA_OWNER_PARTITION)
			Partitions.Maurice -> warmup(datastoreInformation, "by_hcparty_message_date" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}

	}

}
