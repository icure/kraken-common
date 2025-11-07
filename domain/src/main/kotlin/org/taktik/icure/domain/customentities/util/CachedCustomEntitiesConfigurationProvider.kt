package org.taktik.icure.domain.customentities.util

import org.taktik.icure.domain.customentities.config.VersionedCustomEntitiesConfiguration

interface CachedCustomEntitiesConfigurationProvider {
	// Get the configuration that should be used for the current user (normally taken by group id for cloud)
	// This method uses a cache, but may have cache expiration policy
	suspend fun getConfigForCurrentUser(): VersionedCustomEntitiesConfiguration
}