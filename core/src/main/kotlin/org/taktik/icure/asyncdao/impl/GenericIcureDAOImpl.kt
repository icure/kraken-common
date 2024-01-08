/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.ICureDocument
import org.taktik.icure.entities.base.StoredICureDocument

/**
 * @author Antoine Duchâteau
 *
 * Change the behaviour of delete by a soft delete and undelete capabilities
 * Automatically update the modified date
 *
 */
open class GenericIcureDAOImpl<T : StoredICureDocument>(
	entityClass: Class<T>,
	couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	cacheChainLink: EntityCacheChainLink<T>? = null,
	designDocumentProvider: DesignDocumentProvider
) : GenericDAOImpl<T>(entityClass, couchDbDispatcher, idGenerator, cacheChainLink, designDocumentProvider) {
	override suspend fun save(datastoreInformation: IDatastoreInformation, newEntity: Boolean?, entity: T): T? =
		super.save(datastoreInformation, newEntity, entity.apply { setTimestamps(this) })

	override fun saveBulk(
		datastoreInformation: IDatastoreInformation,
		entities: Collection<T>
	): Flow<BulkSaveResult<T>> {
		return super.saveBulk(datastoreInformation, entities.map { it.apply { setTimestamps(this) } })
	}

	override fun <K : Collection<T>> save(datastoreInformation: IDatastoreInformation, newEntity: Boolean?, entities: K): Flow<T> =
		super.save(datastoreInformation, newEntity, entities.map { it.apply { setTimestamps(this) } })

	override suspend fun unRemove(datastoreInformation: IDatastoreInformation, entity: T) =
		super.unRemove(datastoreInformation, entity.apply { setTimestamps(this) })

	override fun unRemove(datastoreInformation: IDatastoreInformation, entities: Collection<T>) =
		super.unRemove(datastoreInformation, entities.map { it.apply { setTimestamps(this) } })

	private fun setTimestamps(entity: ICureDocument<String>) {
		val epochMillis = System.currentTimeMillis()
		if (entity.created == null) {
			entity.withTimestamps(created = epochMillis, modified = epochMillis)
		}
		entity.withTimestamps(modified = epochMillis)
	}
}
