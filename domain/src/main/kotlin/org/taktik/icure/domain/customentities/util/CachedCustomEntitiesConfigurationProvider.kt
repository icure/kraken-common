package org.taktik.icure.domain.customentities.util

import org.taktik.icure.domain.customentities.config.VersionedCustomEntitiesConfiguration

interface CachedCustomEntitiesConfigurationProvider {
	// Get the configuration that should be used for the current user (normally taken by group id for cloud)
	// This method uses a cache, but may have cache expiration policy
	suspend fun getConfigForCurrentUser(): VersionedCustomEntitiesConfiguration
	// Get the configuration that has the provided id
	// This method uses a cache, but may have cache expiration policy
	suspend fun getConfigById(configId: String): VersionedCustomEntitiesConfiguration
	// "Freezes" a cache entry, meaning it will not be evicted by any policy for as long as it is frozen.
	// This is particularly needed for situations like websocket subscription, which keep the reference to the config for
	// the entire duration of the subscription, and it would be wasteful to recreate identical copies instead of reusing
	// the same reference.
	// Freezes are cumulative: if the same config is frozen twice it needs to be unfrozen twice before it can be released.
	// Requires that the passed config is the same instance as the one cached, otherwise throws exception
	fun freezeCacheEntry(config: VersionedCustomEntitiesConfiguration)
	// "Unfreezes" a cache entry
	// Requires that the passed config is the same instance as the one cached, otherwise throws exception
	fun unfreezeCacheEntry(config: VersionedCustomEntitiesConfiguration)
}