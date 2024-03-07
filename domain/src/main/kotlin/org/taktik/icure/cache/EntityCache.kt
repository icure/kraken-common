package org.taktik.icure.cache

import org.taktik.icure.entities.base.StoredDocument

interface EntityCache<T: StoredDocument> {
    /**
     * Gets a single entity. Should return null only if the current link is the last link of the chain, otherwise it
     * should delegate it to the next link.
     * @param id the id of the entity.
     * @return the entity or null, if not found.
     */
    suspend fun getEntity(id: String): T?

    /**
     * Puts an entity in the cache. It should also put the entity in the cache of the next link of the chain.
     * @param id the id of the entity.
     * @param entity the entity.
     */
    suspend fun putInCache(id: String, entity: T)

    /**
     * Removes an entity from the cache by id. It should also remove it from the next link of the chain.
     * @param id the id of the entity to remove.
     */
    suspend fun evictFromCache(id: String)

}