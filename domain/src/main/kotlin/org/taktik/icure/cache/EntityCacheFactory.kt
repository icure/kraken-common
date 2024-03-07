package org.taktik.icure.cache

import org.taktik.icure.entities.base.StoredDocument

/**
 * This component can be used to instantiate cache chains for DAOs.
 */
interface EntityCacheFactory {

    /**
     * Instantiates a cache with a single level that will store the entities in a cache located in memory.
     *
     * @param entityClass the java class of the type of entities to store in the cache.
     */
    fun <T : StoredDocument> localOnlyCache(entityClass: Class<T>): EntityCacheChainLink<T>?

    /**
     * Instantiates a cache with two levels:
     * - The first will store the entities in a cache located in memory.
     * - The second will store the entities in Hazelcast on a miss on the first level.
     *
     * @param entityClass the java class of the type of entities to store in the cache.
     */
    fun <T : StoredDocument> localAndDistributedCache(entityClass: Class<T>): EntityCacheChainLink<T>?
}

inline fun <reified T : StoredDocument> EntityCacheFactory.localOnlyCache(): EntityCacheChainLink<T>? =
    localOnlyCache(T::class.java)

inline fun <reified T : StoredDocument> EntityCacheFactory.localAndDistributedCache(): EntityCacheChainLink<T>? =
    localAndDistributedCache(T::class.java)
