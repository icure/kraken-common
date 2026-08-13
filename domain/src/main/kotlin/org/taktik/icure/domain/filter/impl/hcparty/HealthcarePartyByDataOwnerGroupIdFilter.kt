package org.taktik.icure.domain.filter.impl.hcparty

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.hcparty.HealthcarePartyByDataOwnerGroupIdFilter
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthcarePartyByDataOwnerGroupIdFilter(
	override val dataOwnerGroupId: String,
	override val desc: String? = null,
) : AbstractFilter<HealthcareParty>,
	HealthcarePartyByDataOwnerGroupIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	/**
	 * Membership-only match: whether [item] is directly linked to [dataOwnerGroupId] at all, via legacy [parentId]
	 * or a [dataOwnerGroups] entry.
	 */
	override fun matches(item: HealthcareParty, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		item.parentId == dataOwnerGroupId || item.dataOwnerGroups.any { it.dataOwnerId == dataOwnerGroupId }
}
