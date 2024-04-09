package org.taktik.icure.domain.filter.impl.maintenancetask

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.maintenancetask.MaintenanceTaskByHcPartyAndIdentifiersFilter
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Identifier

data class MaintenanceTaskByHcPartyAndIdentifiersFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val identifiers: List<Identifier>,
) : AbstractFilter<MaintenanceTask>, MaintenanceTaskByHcPartyAndIdentifiersFilter {

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: MaintenanceTask, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return item.endOfLife == null
				&& (healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item))
				&& identifiers.any { searchIdentifier -> item.identifier.any { it.system == searchIdentifier.system && it.id == searchIdentifier.id } }
	}
}
