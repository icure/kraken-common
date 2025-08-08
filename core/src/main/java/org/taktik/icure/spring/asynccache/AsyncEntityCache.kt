package org.taktik.icure.spring.asynccache

import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.entities.base.StoredDocument

/**
 * This class implements a link of the Chain of Responsibility cache based on the AsyncCacheManager.
 */
class AsyncEntityCache<T : StoredDocument>(
	entityClass: Class<T>,
	asyncCacheManager: AsyncCacheManager,
	nextLink: EntityCacheChainLink<T>? = null,
) : EntityCacheChainLink<T>(nextLink) {
	private val cache = asyncCacheManager.getCache<String, T>(entityClass.name)

	override suspend fun evict(id: String) = cache.evict(id)

	override suspend fun get(id: String): T? = cache.get(id)

	override suspend fun put(
		id: String,
		entity: T,
	) = cache.put(id, entity)
}
