package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service

data class ServiceByHcPartyHealthElementIdsFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val healthElementIds: List<String> = emptyList(),
) : AbstractFilter<Service>, org.taktik.icure.domain.filter.service.ServiceByHcPartyHealthElementIdsFilter {

	// This filter requires to check the healthElementId in the parent subContact, a thing that matches cannot do.
	override val canBeUsedInWebsocket = false
	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		throw UnsupportedOperationException("This filter cannot be used in websocket.")
	}
}
