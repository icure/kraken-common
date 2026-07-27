/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.Client
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.Option
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.icure.asyncdao.AttachmentManagementDAO
import org.taktik.icure.asyncdao.BEPPE_PARTITION
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.CustomEntityDAO
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asyncdao.LookupDAO
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.exceptions.TooManyResultsException
import org.taktik.icure.utils.distinct
import org.taktik.icure.utils.distinctById
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.main
import java.io.Serializable
import java.nio.ByteBuffer
import kotlin.collections.map

@Repository("customEntityDAO")
@Profile("app")
@Views(
	View(name = "all_stubs", map = "classpath:js/customentitybase/All_stubs.js"),
)
internal class CustomEntityDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider,
	private val objectMapper: ObjectMapper,
) : GenericDAOImpl<CustomEntityBase>(
	entityClass = CustomEntityBase::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider
), CustomEntityDAO {
	override suspend fun getCustomEntityMetadataStub(datastoreInformation: IDatastoreInformation, id: String): CustomEntityBase? =
		getCustomEntitiesMetadataStubs(datastoreInformation, listOf(id)).firstOrNull()

	override fun getCustomEntitiesMetadataStubs(datastoreInformation: IDatastoreInformation, ids: List<String>): Flow<CustomEntityBase> = flow{
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries =
			createQuery(
				datastoreInformation = datastoreInformation,
				"all_stubs",
			).keys(ids).includeDocs(false)

		client.queryView<String, ObjectNode>(viewQueries).collect { row ->
			val fixedJson = row.value!!.also {
				it.replace("_id", TextNode(row.id))
			}
			emit(objectMapper.treeToValue<CustomEntityBase>(fixedJson))
		}
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
