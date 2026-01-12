package org.taktik.icure.domain.filter.impl.healthelement

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthElementByHcPartyStatusVersioningFilter(
	override val desc: String? = null,
	override val hcPartyId: String,
	override val status: Int,
	override val versionFiltering: VersionFiltering? = null,
) : AbstractFilter<HealthElement>,
	org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyStatusVersioningFilter {

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(hcPartyId)

	override fun matches(item: HealthElement, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		(searchKeyMatcher(hcPartyId, item)) && item.status == status
}

