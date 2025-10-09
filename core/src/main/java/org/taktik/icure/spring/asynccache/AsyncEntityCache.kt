package org.taktik.icure.spring.asynccache

import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.entities.base.StoredDocument
import java.io.Serializable

/**
 * This class implements a link of the Chain of Responsibility cache based on the AsyncCacheManager.
 */
class AsyncEntityCache<K : Serializable, T : StoredDocument>(
	entityClass: Class<T>,
	asyncCacheManager: AsyncCacheManager,
	nextLink: EntityCacheChainLink<K, T>? = null,
) : EntityCacheChainLink<K, T>(nextLink) {
	private val cache = asyncCacheManager.getCache<K, T>(entityClass.name)

	override suspend fun evict(id: K) = cache.evict(id)

	override suspend fun get(id: K): T? = cache.get(id)

	override suspend fun put(
		id: K,
		entity: T,
	) = cache.put(id, entity)
}
