package org.taktik.icure.dao

import org.springframework.stereotype.Component
import org.taktik.couchdb.Client
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.dao.designDocName
import org.taktik.couchdb.entity.NullKey
import org.taktik.couchdb.entity.ViewQuery
import org.taktik.icure.asyncdao.impl.GenericDAOImpl
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.utils.createQuery
import org.taktik.icure.utils.pagedViewQuery

@Component
class QueryProvider(
	private val designDocumentProvider: DesignDocumentProvider,
	private val designDocSchemaCache: DesignDocSchemaCache
) {

	private suspend fun createQueryFromSchema(
		entityClass: Class<*>,
		viewName: String,
	): ViewQuery? = designDocSchemaCache.getOrRequestSchema()?.let { schema ->
		val viewsForEntity = schema.viewsByEntity[entityClass.simpleName]
		val partition = requireNotNull(
			viewsForEntity?.get(viewName)
		) { "$viewName not found in configuration for ${entityClass.simpleName}" }
		ViewQuery()
			.designDocId(designDocName(entityClass.simpleName, partition.toString()))
			.skipIfViewDoesNotExist(false)
			.viewName(viewName)
	}

	private suspend fun <P>  createPagedQueryFromSchema(
		entityClass: Class<*>,
		viewName: String,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
		descending: Boolean
	): ViewQuery? = createQueryFromSchema(entityClass = entityClass, viewName = viewName)
		?.startKey(pagination.startKey ?: startKey ?: NullKey)
		?.endKey(endKey)
		?.includeDocs(true)
		?.reduce(false)
		?.startDocId(pagination.startDocumentId)
		?.limit(pagination.limit)
		?.descending(descending)

	context(dao: GenericDAOImpl<*>)
	suspend fun createQuery(
		client: Client,
		legacyReference: DesignDocReference.LegacyReference,
		configurationReference: DesignDocReference.ConfigurationReference? = null
	): ViewQuery = configurationReference?.let { configReference ->
			createQueryFromSchema(entityClass = dao.entityClass, viewName = configReference.viewName)
		} ?: designDocumentProvider.createQuery(
			client = client,
			metadataSource = dao,
			viewName = legacyReference.viewName,
			entityClass = dao.entityClass,
			secondaryPartition = legacyReference.secondaryPartition
		)

	context(dao: GenericDAOImpl<*>)
	suspend fun <P> pagedViewQuery(
		client: Client,
		legacyReference: DesignDocReference.LegacyReference,
		configurationReference: DesignDocReference.ConfigurationReference? = null,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
		descending: Boolean
	): ViewQuery = configurationReference?.let { configReference ->
			createPagedQueryFromSchema(
				entityClass = dao.entityClass,
				viewName = configReference.viewName,
				startKey = startKey,
				endKey = endKey,
				pagination = pagination,
				descending = descending,
			)
		} ?: designDocumentProvider.pagedViewQuery(
			client = client,
			metadataSource = dao,
			viewName = legacyReference.viewName,
			entityClass = dao.entityClass,
			startKey = startKey,
			endKey = endKey,
			pagination = pagination,
			descending = descending,
			secondaryPartition = legacyReference.secondaryPartition
		)
}

sealed interface DesignDocReference {
	data class LegacyReference(val viewName: String, val secondaryPartition: String?) : DesignDocReference
	data class ConfigurationReference(val viewName: String): DesignDocReference
}