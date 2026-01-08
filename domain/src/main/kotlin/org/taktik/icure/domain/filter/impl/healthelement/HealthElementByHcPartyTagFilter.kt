package org.taktik.icure.domain.filter.impl.healthelement

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.containsStubWithTypeAndCode

data class HealthElementByHcPartyTagFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String,
	override val tagType: String,
	override val tagCode: String,
	override val startOfHealthElementDate: Long? = null,
	override val endOfHealthElementDate: Long? = null,
	override val versionFiltering: VersionFiltering? = null,
) : AbstractFilter<HealthElement>,
	org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyTagFilter {

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(healthcarePartyId)

	override fun matches(item: HealthElement, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		if (!searchKeyMatcher(healthcarePartyId, item)) return false
		if (!item.tags.containsStubWithTypeAndCode(tagType, tagCode)) return false
		val date = item.valueDate ?: item.openingDate
		if (startOfHealthElementDate != null && (date == null || date < startOfHealthElementDate)) return false
		if (endOfHealthElementDate != null && (date == null || date > endOfHealthElementDate)) return false
		return true
	}
}

