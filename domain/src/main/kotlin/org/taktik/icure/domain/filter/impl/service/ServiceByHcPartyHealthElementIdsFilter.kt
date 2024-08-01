package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.withEncryptionMetadata

data class ServiceByHcPartyHealthElementIdsFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val healthElementIds: List<String> = emptyList(),
) : AbstractFilter<Service>, org.taktik.icure.domain.filter.service.ServiceByHcPartyHealthElementIdsFilter {

	override val canBeUsedInWebsocket = true
	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return (
			item.endOfLife == null && (healthcarePartyId == null || item.withEncryptionMetadata()?.let { searchKeyMatcher(healthcarePartyId, it) } == true) &&
				healthElementIds.any { healthElementId ->
					item.healthElementsIds?.any { it == healthElementId } ?: false
				}
			)
	}
}
