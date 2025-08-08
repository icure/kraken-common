package org.taktik.icure.domain.filter.impl.healthelement

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthElementByHcPartyStatusFilter(
	override val desc: String? = null,
	override val hcPartyId: String,
	override val status: Int,
) : AbstractFilter<HealthElement>,
	org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyStatusFilter {

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(hcPartyId)

	override fun matches(item: HealthElement, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = ((searchKeyMatcher(hcPartyId, item)) && item.status == status)
}
