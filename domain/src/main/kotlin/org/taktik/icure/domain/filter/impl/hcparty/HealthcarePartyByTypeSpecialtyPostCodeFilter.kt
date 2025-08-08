package org.taktik.icure.domain.filter.impl.hcparty

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.hcparty.HealthcarePartyByTypeSpecialtyPostCodeFilter
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthcarePartyByTypeSpecialtyPostCodeFilter(
	override val specialty: String,
	override val specCode: String,
	override val startPostCode: String,
	override val endPostCode: String,
	override val desc: String? = null,
) : AbstractFilter<HealthcareParty>,
	HealthcarePartyByTypeSpecialtyPostCodeFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: HealthcareParty, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.speciality == specialty &&
		item.nihiiSpecCode == specCode &&
		item.addresses.any {
			it.postalCode != null && it.postalCode >= startPostCode && it.postalCode <= endPostCode
		}
}
