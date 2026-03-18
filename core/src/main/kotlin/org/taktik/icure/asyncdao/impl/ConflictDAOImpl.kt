package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.couchdb.queryViewNoValue
import org.taktik.icure.asyncdao.ConflictDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.StoredDocument

abstract class ConflictDAOImpl<T: StoredDocument>(
	entityClass: Class<T>,
	couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	cacheChain: EntityCacheChainLink<String, T>? = null,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : ConflictDAO<T>, GenericDAOImpl<T>(entityClass, couchDbDispatcher, idGenerator, cacheChain, designDocumentProvider, daoConfig) {

	protected inline fun <reified E: T> doListIdsOfEntitiesWithConflicts(
		datastoreInformation: IDatastoreInformation,
		viewName: String,
		partition: String? = null
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, viewName, partition).includeDocs(false)
		emitAll(client.queryViewNoValue<String>(viewQuery).map { it.id })
	}

	protected inline fun <reified E: T> doListConflicts(
		datastoreInformation: IDatastoreInformation,
		viewName: String,
		partition: String? = null
	): Flow<T> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, viewName, partition).includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, E>(viewQuery).map { it.doc })
	}

	override fun purgeConflictingRevisions(
		datastoreInformation: IDatastoreInformation,
		entityId: String,
		revisionsToPurge: List<String>
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val conflictsToPurge = revisionsToPurge.map {
			IdAndRev(id = entityId, rev = it)
		}
		emitAll(
			client.bulkDeleteByIdAndRev(entities = conflictsToPurge).toSaveResult { id, rev ->
				DocIdentifier(id, rev)
			}
		)
	}

}