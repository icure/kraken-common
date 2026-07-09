package org.taktik.icure.spring.asynccache

import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.utils.addServerTimingHeader
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

	override suspend fun evict(id: K) {
		val start = System.currentTimeMillis()
		cache.evict(id)
		addServerTimingHeader(
			name = "hzc-del-$id",
			duration = System.currentTimeMillis() - start,
			methodCallStart = start
		)
	}

	override suspend fun get(id: K): T? {
		val start = System.currentTimeMillis()
		return cache.get(id).also {
			addServerTimingHeader(
				name = "hzc-get-$id",
				duration = System.currentTimeMillis() - start,
				methodCallStart = start
			)
		}
	}

	override suspend fun put(
		id: K,
		entity: T,
	) {
		val start = System.currentTimeMillis()
		cache.put(id, entity)
		addServerTimingHeader(
			name = "hzc-put-$id",
			duration = System.currentTimeMillis() - start,
			methodCallStart = start
		)
	}
}
