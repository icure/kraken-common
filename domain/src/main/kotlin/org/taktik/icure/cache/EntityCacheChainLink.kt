package org.taktik.icure.cache

import org.taktik.icure.entities.base.StoredDocument

/**
 * Implementation of the EntityCache interface as a Chain of Responsibility where the first link is the
 * fastest cache (e.g. memory) while the last is the slowest (e.g. HazelCast or the DB).
 * In the request phase, each link should return the entity if it is present in its cache or delegate it to the next
 * link. In the response phase, it should put in its cache any entity that it is not there already.
 */
abstract class EntityCacheChainLink<T: StoredDocument>(
    // The next link of the chain.
    private val nextLink: EntityCacheChainLink<T>? = null
): EntityCache<T> {

    final override suspend fun getEntity(id: String): T? =
        get(id) ?: nextLink?.getEntity(id)?.also {
            put(id, it)
        }

    final override suspend fun evictFromCache(id: String) {
        evict(id)
        nextLink?.evictFromCache(id)
    }

    final override suspend fun putInCache(id: String, entity: T) {
        put(id, entity)
        nextLink?.putInCache(id, entity)
    }

    /**
     * Gets an element from the cache by its id.
     * @param id the id of the element to retrieve.
     * @return the element or null if not present in the cache.
     */
    protected abstract suspend fun get(id: String): T?

    /**
     * Stores an element in the cache.
     * @param id the id of the element to store.
     * @param entity the entity to store.
     */
    protected abstract suspend fun put(id: String, entity: T)

    /**
     * Removes an element from the cache.
     * @param id the id of the element to remove.
     */
    protected abstract suspend fun evict(id: String)
}