package org.taktik.icure.dao

import org.taktik.couchdb.dao.designDocName
import org.taktik.couchdb.entity.NullKey
import org.taktik.couchdb.entity.ViewQuery
import org.taktik.icure.asyncdao.DAOWithClass
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.exceptions.MissingViewException
import org.taktik.icure.utils.NoDocViewQueries
import org.taktik.icure.utils.ViewQueries

class QueryProvider(
	private val designDocSchemaProvider: DesignDocSchemaProvider,
	private val failOnMissingView: Boolean
) {

	/**
	 * Instantiates a [ViewQuery] if a DesignDocSchema is defined in the group of the current user, returning null
	 * otherwise.
	 * If a schema is defined but no view with the specified name is defined for the entity and [failOnMissingView] is
	 * true, it throws an exception. If the view is missing and [failOnMissingView] is false, it returns null that will
	 * make the query fall back to the default design doc.
	 * [failOnMissingView] can be true to allow a progressive migration of the view for the database that are already indexed,
	 * to allow a fallback to default in case a specific view was not migrated yet.
	 */
	private suspend fun createQueryFromSchema(
		datastore: IDatastoreInformation,
		entityClass: Class<*>,
		viewName: String,
	): ViewQuery? = designDocSchemaProvider.getOrRequestSchema(datastore)?.let { schema ->
		val viewsForEntity = schema.viewsByEntity[entityClass.simpleName]
		val partition = viewsForEntity?.get(viewName)
		when {
			partition != null -> ViewQuery()
				.designDocId(designDocName(entityClass.simpleName, partition.toString()))
				.skipIfViewDoesNotExist(false)
				.viewName(viewName)
			failOnMissingView -> throw MissingViewException(viewName = viewName, entity = entityClass.simpleName, schema.id)
			else -> null
		}
	}

	/**
	 * Instantiates a [NoDocViewQueries] if a DesignDocSchema is defined in the group of the current user, returning null
	 * otherwise.
	 * If a schema is defined but any view with the specified name is not defined for the entity, it throws an exception.
	 * The [NoDocViewQueries] created with this method will always have only one query inside: this is intentional to
	 * maintain compatibility with the legacy interleave mechanism (that is also optimized not to interleave view queries
	 * with a single query)
	 */
	private suspend fun createQueriesFromSchema(
		datastore: IDatastoreInformation,
		entityClass: Class<*>,
		viewName: String,
	): NoDocViewQueries? = createQueryFromSchema(
		datastore = datastore,
		entityClass = entityClass,
		viewName = viewName
	)?.let {
		NoDocViewQueries(listOf(it))
	}

	private suspend fun <P>  createPagedQueryFromSchema(
		datastore: IDatastoreInformation,
		entityClass: Class<*>,
		viewName: String,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
		descending: Boolean
	): ViewQuery? = createQueryFromSchema(entityClass = entityClass, viewName = viewName, datastore = datastore)
		?.startKey(pagination.startKey ?: startKey ?: NullKey)
		?.endKey(endKey)
		?.includeDocs(true)
		?.reduce(false)
		?.startDocId(pagination.startDocumentId)
		?.limit(pagination.limit)
		?.descending(descending)

	private suspend fun <P>  createPagedQueryOfIdsFromSchema(
		datastore: IDatastoreInformation,
		entityClass: Class<*>,
		viewName: String,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>
	): ViewQuery? = createQueryFromSchema(entityClass = entityClass, viewName = viewName, datastore = datastore)
		?.startKey(pagination.startKey ?: startKey ?: NullKey)
		?.endKey(endKey)
		?.startDocId(pagination.startDocumentId)
		?.includeDocs(false)
		?.reduce(false)
		?.limit(pagination.limit)

	private suspend fun <P> createPagedQueriesFromSchema(
		datastore: IDatastoreInformation,
		entityClass: Class<*>,
		viewName: String,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
		descending: Boolean
	): ViewQueries? = designDocSchemaProvider.getOrRequestSchema(datastore)?.let { schema ->
		val viewsForEntity = schema.viewsByEntity[entityClass.simpleName]
		val partition = viewsForEntity?.get(viewName)
		when {
			partition != null -> ViewQueries(
				listOf(
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
				)
			)
			failOnMissingView -> throw MissingViewException(viewName = viewName, entity = entityClass.simpleName, schema.id)
			else -> null
		}
	}

	context(dao: DAOWithClass<*>)
	suspend fun hasAllViewForCurrentEntity(datastore: IDatastoreInformation): Boolean? =
		designDocSchemaProvider.getOrRequestSchema(datastore)?.let { schema ->
			val viewsForEntity = schema.viewsByEntity[dao.entityClass.simpleName]
			when {
				viewsForEntity?.containsKey("all") == true -> true
				failOnMissingView -> throw MissingViewException(viewName = "all", entity = dao.entityClass.simpleName, schema.id)
				else -> false
			}
		}

	context(dao: DAOWithClass<*>)
	suspend fun createQuery(
		datastore: IDatastoreInformation,
		configurationView: String,
	): ViewQuery? = createQueryFromSchema(
		entityClass = dao.entityClass,
		viewName = configurationView,
		datastore = datastore
	)

	context(dao: DAOWithClass<*>)
	suspend fun createQueryFromSchemaOrNull(
		datastore: IDatastoreInformation,
		configurationView: String,
	): ViewQuery? = createQueryFromSchema(
		entityClass = dao.entityClass,
		viewName = configurationView,
		datastore = datastore
	)

	context(dao: DAOWithClass<*>)
	suspend fun <P> pagedViewQuery(
		datastore: IDatastoreInformation,
		configurationView: String,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
		descending: Boolean
	): ViewQuery? = createPagedQueryFromSchema(
		entityClass = dao.entityClass,
		datastore = datastore,
		viewName = configurationView,
		startKey = startKey,
		endKey = endKey,
		pagination = pagination,
		descending = descending,
	)

	context(dao: DAOWithClass<*>)
	suspend fun createQueries(
		datastore: IDatastoreInformation,
		configurationView: String,
	): NoDocViewQueries? = createQueriesFromSchema(
		entityClass = dao.entityClass,
		viewName = configurationView,
		datastore = datastore
	)

	context(dao: DAOWithClass<*>)
	suspend fun <P> createPagedQueries(
		datastore: IDatastoreInformation,
		configurationView: String,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
		descending: Boolean
	): ViewQueries? = createPagedQueriesFromSchema(
		entityClass = dao.entityClass,
		viewName = configurationView,
		datastore = datastore,
		startKey = startKey,
		endKey = endKey,
		pagination = pagination,
		descending = descending,
	)

	context(dao: DAOWithClass<*>)
	suspend fun <P> pagedViewQueryOfIds(
		datastore: IDatastoreInformation,
		configurationView: String,
		startKey: P?,
		endKey: P?,
		pagination: PaginationOffset<P>,
	): ViewQuery? = createPagedQueryOfIdsFromSchema(
		entityClass = dao.entityClass,
		datastore = datastore,
		viewName = configurationView,
		startKey = startKey,
		endKey = endKey,
		pagination = pagination,
	)
}