/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.CustomEntityDAO
import org.taktik.icure.cache.CustomEntityCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.CustomEntityBase
import java.nio.ByteBuffer

@Repository("customEntityDAO")
@Profile("app")
@Views(
	View(name = "all_stubs", map = "classpath:js/customentitybase/All_stubs.js"),
)
internal class CustomEntityDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider,
	private val objectMapper: ObjectMapper,
	override val cacheChain: CustomEntityCache
) : GenericDAOImpl<CustomEntityBase>(
	entityClass = CustomEntityBase::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = cacheChain,
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider
), CustomEntityDAO {
	companion object {
		private val log = LoggerFactory.getLogger(CustomEntityDAOImpl::class.java)
	}

	override suspend fun getCustomEntityMetadataStub(datastoreInformation: IDatastoreInformation, id: String): CustomEntityBase? =
		getCustomEntitiesMetadataStubs(datastoreInformation, listOf(id)).firstOrNull()

	override fun getCustomEntitiesMetadataStubs(datastoreInformation: IDatastoreInformation, ids: List<String>): Flow<CustomEntityBase> = flow{
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val deduped = ids.distinct()
		val existingStubs = deduped.mapNotNull { cacheChain.getStub(datastoreInformation.getFullIdFor(it)) }.associateByTo(mutableMapOf()) { it.id }
		val missingStubs = deduped.filter { !existingStubs.containsKey(it) }

		if (missingStubs.isEmpty()) {
			deduped.forEach { emit(existingStubs.getValue(it)) }
			return@flow
		}

		val viewQueries =
			createQuery(
				datastoreInformation = datastoreInformation,
				"all_stubs",
			).keys(missingStubs).includeDocs(false)

		var resultIndex = 0
		var warnedAboutOrder = false

		client.queryView<String, ObjectNode>(viewQueries).collect { row ->
			while (resultIndex < deduped.size && deduped[resultIndex] != row.id) {
				existingStubs.remove(deduped[resultIndex])?.also {
					emit(it)
				}
				resultIndex++
			}
			if (resultIndex >= deduped.size && !warnedAboutOrder) {
				// The row was not found at or after the current position in the requested ids, meaning that couchdb did
				// not return the rows in the order of the provided keys. All the existing stubs are still emitted
				// exactly once, but the order of the output no longer matches the order of the input.
				warnedAboutOrder = true
				log.warn("The all_stubs view returned the row for ${row.id} out of the requested keys order: custom entity stubs will be emitted in an arbitrary order")
			}
			val fixedJson = row.value!!.also {
				it.replace("_id", TextNode(row.id))
			}
			val parsed = objectMapper.treeToValue<CustomEntityBase>(fixedJson)
			cacheChain.putStub(datastoreInformation.getFullIdFor(row.id), parsed)
			emit(parsed)
			resultIndex++
		}
		while (resultIndex < deduped.size) {
			existingStubs.remove(deduped[resultIndex])?.also {
				emit(it)
			}
			resultIndex++
		}
		existingStubs.values.forEach { emit(it) }
	}

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
