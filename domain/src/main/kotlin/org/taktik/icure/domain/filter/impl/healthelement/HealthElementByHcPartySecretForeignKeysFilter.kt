package org.taktik.icure.domain.filter.impl.healthelement

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthElementByHcPartySecretForeignKeysFilter(
	override val desc: String?,
	override val healthcarePartyId: String?,
	override val patientSecretForeignKeys: Set<String> = emptySet()
) : AbstractFilter<HealthElement>, org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartySecretForeignKeysFilter {

	override val canBeUsedInWebsocket = true
	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: HealthElement, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return (
			(healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item)) &&
				item.secretForeignKeys.any { patientSecretForeignKeys.contains(it) }
			)
	}
}
