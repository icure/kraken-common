/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.datastore.IDatastoreInformation
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
	cacheChainLink: EntityCacheChainLink<String, T>? = null,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
) : GenericDAOImpl<T>(entityClass, couchDbDispatcher, idGenerator, cacheChainLink, designDocumentProvider, daoConfig = daoConfig) {
	override suspend fun save(
		datastoreInformation: IDatastoreInformation,
		newEntity: Boolean?,
		entity: T,
	): T? = super.save(datastoreInformation, newEntity, entity.apply { setTimestamps(this) })

	override fun createBulk(
		datastoreInformation: IDatastoreInformation,
		entities: Collection<T>
	): Flow<BulkSaveResult<T>> = saveBulk(datastoreInformation,entities.also {
		require(it.all { e -> e.rev == null }) { "All entities must have null rev for creation" }
	})

	override fun saveBulk(
		datastoreInformation: IDatastoreInformation,
		entities: Collection<T>,
	): Flow<BulkSaveResult<T>> = super.saveBulk(datastoreInformation, entities.map { it.apply { setTimestamps(this) } })

	override fun unRemove(
		datastoreInformation: IDatastoreInformation,
		entities: Collection<T>,
	) = super.unRemove(datastoreInformation, entities.map { it.apply { setTimestamps(this) } })

	private fun setTimestamps(entity: ICureDocument<String>) {
		val epochMillis = System.currentTimeMillis()
		if (entity.created == null) {
			entity.withTimestamps(created = epochMillis, modified = epochMillis)
		}
		entity.withTimestamps(modified = epochMillis)
	}
}
