package org.taktik.icure.domain.filter.impl.hcparty

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.hcparty.HealthcarePartyByNationalIdentifierFilter
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthcarePartyByNationalIdentifierFilter(
	override val searchValue: String,
	override val descending: Boolean? = null,
	override val desc: String? = null,
) : AbstractFilter<HealthcareParty>,
	HealthcarePartyByNationalIdentifierFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: HealthcareParty, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = listOfNotNull(item.ssin, item.nihii, item.cbe, item.ehp).any { it.startsWith(searchValue) }
}
