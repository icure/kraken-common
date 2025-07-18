package org.taktik.icure.domain.filter.impl.healthelement

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthElementByDataOwnerPatientOpeningDate(
	override val desc: String? = null,
	override val healthcarePartyId: String,
	override val patientSecretForeignKeys: Set<String> = emptySet(),
	override val startDate: Long? = null,
	override val endDate: Long? = null,
	override val descending: Boolean = false,

) : AbstractFilter<HealthElement>,
	org.taktik.icure.domain.filter.healthelement.HealthElementByDataOwnerPatientOpeningDate {

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(healthcarePartyId)

	override fun matches(item: HealthElement, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = (
		(
			searchKeyMatcher(
				healthcarePartyId,
				item,
			)
			) &&
			item.secretForeignKeys.intersect(patientSecretForeignKeys).isNotEmpty() &&
			(startDate == null || (item.openingDate != null && item.openingDate >= startDate)) &&
			(endDate == null || (item.openingDate != null && item.openingDate <= endDate))
		)
}
