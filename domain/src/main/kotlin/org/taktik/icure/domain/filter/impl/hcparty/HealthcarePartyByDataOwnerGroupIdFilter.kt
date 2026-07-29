package org.taktik.icure.domain.filter.impl.hcparty

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.hcparty.HealthcarePartyByDataOwnerGroupIdFilter
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthcarePartyByDataOwnerGroupIdFilter(
	override val dataOwnerGroupId: String,
	override val linkType: DataOwnerGroupLinkType? = null,
	override val desc: String? = null,
) : AbstractFilter<HealthcareParty>,
	HealthcarePartyByDataOwnerGroupIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: HealthcareParty, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		val effectiveLinkType =
			if (item.parentId == dataOwnerGroupId) {
				DataOwnerGroupLinkType.parent
			} else {
				item.dataOwnerGroups.firstOrNull { it.dataOwnerId == dataOwnerGroupId }?.linkType
			}
		return effectiveLinkType != null && (linkType == null || linkType == effectiveLinkType)
	}
}
