package org.taktik.icure.cache

import org.taktik.icure.entities.base.StoredDocument
import java.io.Serializable

/**
 * Implementation of the EntityCache interface as a Chain of Responsibility where the first link is the
 * fastest cache (e.g. memory) while the last is the slowest (e.g. HazelCast or the DB).
 * In the request phase, each link should return the entity if it is present in its cache or delegate it to the next
 * link. In the response phase, it should put in its cache any entity that it is not there already.
 */
abstract class EntityCacheChainLink<K : Serializable, T : StoredDocument>(
	// The next link of the chain.
	private val nextLink: EntityCacheChainLink<K, T>? = null,
) : EntityCache<K, T> {

	final override suspend fun getEntity(id: K): T? = get(id) ?: nextLink?.getEntity(id)?.also {
		put(id, it)
	}

	final override suspend fun evictFromCache(id: K) {
		evict(id)
		nextLink?.evictFromCache(id)
	}

	final override suspend fun putInCache(id: K, entity: T) {
		put(id, entity)
		nextLink?.putInCache(id, entity)
	}

	/**
	 * Gets an element from the cache by its id.
	 * @param id the id of the element to retrieve.
	 * @return the element or null if not present in the cache.
	 */
	protected abstract suspend fun get(id: K): T?

	/**
	 * Stores an element in the cache.
	 * @param id the id of the element to store.
	 * @param entity the entity to store.
	 */
	protected abstract suspend fun put(id: K, entity: T)

	/**
	 * Removes an element from the cache.
	 * @param id the id of the element to remove.
	 */
	protected abstract suspend fun evict(id: K)
}
