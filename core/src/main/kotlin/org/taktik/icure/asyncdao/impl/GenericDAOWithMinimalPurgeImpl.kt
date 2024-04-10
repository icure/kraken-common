/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.GenericDAOWithMinimalPurge
import org.taktik.icure.asyncdao.results.toBulkSaveResultFailure
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.base.StoredDocument

abstract class GenericDAOWithMinimalPurgeImpl<T : StoredDocument>(
	entityClass: Class<T>,
	couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	cacheChain: EntityCacheChainLink<T>? = null,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<T>(
	entityClass,
	couchDbDispatcher,
	idGenerator,
	cacheChain,
	designDocumentProvider,
	daoConfig = daoConfig
), GenericDAOWithMinimalPurge<T> {
	override fun purgeByIdAndRev(
		datastoreInformation: IDatastoreInformation,
		idsAndRevs: Collection<IdAndRev>
	) = flow {
		emitAll(couchDbDispatcher.getClient(datastoreInformation).bulkDeleteByIdAndRev(idsAndRevs).map { res ->
			res.toBulkSaveResultFailure()?.entityOrThrow() ?: DocIdentifier(res.id, res.rev).also {
				cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(res.id))
			}
		})
	}

	final override suspend fun beforeDelete(datastoreInformation: IDatastoreInformation, entity: T): T {
		return entity
	}

	final override suspend fun afterDelete(datastoreInformation: IDatastoreInformation, entity: T): T {
		return entity
	}
}
