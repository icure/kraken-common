package org.taktik.icure.dao

import org.springframework.stereotype.Component
import org.taktik.couchdb.Client
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.dao.designDocName
import org.taktik.couchdb.entity.NullKey
import org.taktik.couchdb.entity.ViewQuery
import org.taktik.icure.asyncdao.DAOWithClass
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.exceptions.MissingViewException
import org.taktik.icure.utils.NoDocViewQueries
import org.taktik.icure.utils.ViewQueries
import org.taktik.icure.utils.createPagedQueries
import org.taktik.icure.utils.createQueries
import org.taktik.icure.utils.createQuery
import org.taktik.icure.utils.pagedViewQuery
import org.taktik.icure.utils.pagedViewQueryOfIds

@Component
class QueryProvider(
	private val designDocumentProvider: DesignDocumentProvider,
	private val designDocSchemaCache: DesignDocSchemaCache
) {

	/**
	 * Instantiates a [ViewQuery] if a DesignDocSchema is defined in the group of the current user, returning null
	 * otherwise.
	 * If a schema is defined but no view with the specified name is defined for the entity, it throws an exception.
	 */
	private suspend fun createQueryFromSchema(
		entityClass: Class<*>,
		viewName: String,
	): ViewQuery? = designDocSchemaCache.getOrRequestSchema()?.let { schema ->
		val viewsForEntity = schema.viewsByEntity[entityClass.simpleName]
		val partition = viewsForEntity?.get(viewName)
			?: throw MissingViewException(viewName = viewName, entity = entityClass.simpleName, schema.id)
		ViewQuery()
			.designDocId(designDocName(entityClass.simpleName, partition.toString()))
			.skipIfViewDoesNotExist(false)
			.viewName(viewName)
	}

	/**
	 * Instantiates a [NoDocViewQueries] if a DesignDocSchema is defined in the group of the current user, returning null
	 * otherwise.
	 * If a schema is defined but any view with the specified name is not defined for the entity, it throws an exception.
	 */
	private suspend fun createQueriesFromSchema(
		entityClass: Class<*>,
		viewNames: List<String>,
	): NoDocViewQueries? = designDocSchemaCache.getOrRequestSchema()?.let { schema ->
		val viewsForEntity = schema.viewsByEntity[entityClass.simpleName]
		NoDocViewQueries(
			viewNames.map { viewName ->
				val partition = viewsForEntity?.get(viewName)
					?: throw MissingViewException(viewName = viewName, entity = entityClass.simpleName, schema.id)
				ViewQuery()
					.designDocId(designDocName(entityClass.simpleName, partition.toString()))
					.skipIfViewDoesNotExist(false)
					.viewName(viewName)
			}
		)
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

	private suspend fun <P>  createPagedQueryOfIdsFromSchema(
		entityClass: Class<*>,
		viewName: String,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>
	): ViewQuery? = createQueryFromSchema(entityClass = entityClass, viewName = viewName)
		?.startKey(pagination.startKey ?: startKey ?: NullKey)
		?.endKey(endKey)
		?.startDocId(pagination.startDocumentId)
		?.includeDocs(false)
		?.reduce(false)
		?.limit(pagination.limit)

	private suspend fun <P> createPagedQueriesFromSchema(
		entityClass: Class<*>,
		viewNames: List<String>,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
		descending: Boolean
	): ViewQueries? = designDocSchemaCache.getOrRequestSchema()?.let { schema ->
		val viewsForEntity = schema.viewsByEntity[entityClass.simpleName]
		ViewQueries(
			viewNames.map { viewName ->
				val partition = requireNotNull(viewsForEntity?.get(viewName)) {
					"$viewName not found in configuration for ${entityClass.simpleName}"
				}
				ViewQuery()
					.designDocId(designDocName(entityClass.simpleName, partition.toString()))
					.skipIfViewDoesNotExist(false)
					.viewName(viewName)
					.startKey(pagination.startKey ?: startKey ?: NullKey)
					.endKey(endKey)
					.includeDocs(true)
					.reduce(false)
					.startDocId(pagination.startDocumentId)
					.limit(pagination.limit)
					.descending(descending)
			}
		)
	}

	context(dao: DAOWithClass<*>)
	suspend fun hasAllViewForCurrentEntity(): Boolean =
		designDocSchemaCache.getOrRequestSchema()
			?.viewsByEntity
			?.get(dao.entityClass.simpleName)
			?.containsKey("all") ?: false

	context(dao: DAOWithClass<*>)
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

	context(dao: DAOWithClass<*>)
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

	context(dao: DAOWithClass<*>)
	suspend fun createQueries(
		client: Client,
		legacyReferences: DesignDocReference.LegacyReferences,
		configurationReferences: List<DesignDocReference.ConfigurationReference>? = null
	): NoDocViewQueries = configurationReferences?.let { configReferences ->
			createQueriesFromSchema(
				entityClass = dao.entityClass,
				viewNames = configReferences.map { it.viewName }
			)
		} ?: designDocumentProvider.createQueries(
			client = client,
			metadataSource = dao,
			clazz = dao.entityClass,
			useDataOwner = legacyReferences.useDataOwnerPartition,
			*legacyReferences.viewQueries.toTypedArray()
		)

	context(dao: DAOWithClass<*>)
	suspend fun <P> createPagedQueries(
		client: Client,
		legacyReferences: DesignDocReference.LegacyReferences,
		configurationReferences: List<DesignDocReference.ConfigurationReference>? = null,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
		descending: Boolean
	): ViewQueries =
		configurationReferences?.let { configReferences ->
			createPagedQueriesFromSchema(
				entityClass = dao.entityClass,
				viewNames = configReferences.map { it.viewName },
				startKey = startKey,
				endKey = endKey,
				pagination = pagination,
				descending = descending,
			)
		} ?: designDocumentProvider.createPagedQueries(
			client = client,
			metadataSource = dao,
			clazz = dao.entityClass,
			viewQueries = legacyReferences.viewQueries,
			startKey = startKey,
			endKey = endKey,
			pagination = pagination,
			descending = descending,
			useDataOwner = legacyReferences.useDataOwnerPartition
		)

	context(dao: DAOWithClass<*>)
	suspend fun <P> pagedViewQueryOfIds(
		client: Client,
		legacyReference: DesignDocReference.LegacyReference,
		configurationReference: DesignDocReference.ConfigurationReference? = null,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
	): ViewQuery = configurationReference?.let { configReference ->
		createPagedQueryOfIdsFromSchema(
			entityClass = dao.entityClass,
			viewName = configReference.viewName,
			startKey = startKey,
			endKey = endKey,
			pagination = pagination,
		)
	} ?: designDocumentProvider.pagedViewQueryOfIds(
		client = client,
		metadataSource = dao.entityClass,
		viewName = legacyReference.viewName,
		entityClass = dao.entityClass,
		startKey = startKey,
		endKey = endKey,
		pagination = pagination,
		secondaryPartition = legacyReference.secondaryPartition
	)
}

sealed interface DesignDocReference {
	data class LegacyReferences(val useDataOwnerPartition: Boolean, val viewQueries: List<Pair<String, String?>>) : DesignDocReference
	data class LegacyReference(val viewName: String, val secondaryPartition: String?) : DesignDocReference
	data class ConfigurationReference(val viewName: String): DesignDocReference
}