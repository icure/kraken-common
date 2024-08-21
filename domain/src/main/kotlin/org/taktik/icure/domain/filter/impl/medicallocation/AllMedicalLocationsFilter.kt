package org.taktik.icure.domain.filter.impl.medicallocation

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.MedicalLocation
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class AllMedicalLocationsFilter(
	override val desc: String? = null,
) : AbstractFilter<MedicalLocation>, Filters.AllFilter<String, MedicalLocation> {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: MedicalLocation, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = true
}
