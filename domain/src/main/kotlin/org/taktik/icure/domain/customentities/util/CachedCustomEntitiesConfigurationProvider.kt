package org.taktik.icure.domain.customentities.util

import org.taktik.icure.domain.customentities.config.VersionedCustomEntitiesConfiguration

interface CachedCustomEntitiesConfigurationProvider {
	fun getCurrentUserCustomConfig(): VersionedCustomEntitiesConfiguration


	// Add cache node, cache node has null or reference
}