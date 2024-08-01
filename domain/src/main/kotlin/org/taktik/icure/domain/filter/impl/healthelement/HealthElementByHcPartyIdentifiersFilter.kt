package org.taktik.icure.domain.filter.impl.healthelement

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Identifier

data class HealthElementByHcPartyIdentifiersFilter(
	override val desc: String?,
	override val hcPartyId: String?,
	override val identifiers: List<Identifier>
) : AbstractFilter<HealthElement>, org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyIdentifiersFilter {

	override val canBeUsedInWebsocket = true
	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = hcPartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: HealthElement, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return ((hcPartyId == null || searchKeyMatcher(hcPartyId, item)) &&
				identifiers.any { searchIdentifier -> item.identifiers.any { it.system == searchIdentifier.system && it.id == searchIdentifier.id } }
			)
	}
}
