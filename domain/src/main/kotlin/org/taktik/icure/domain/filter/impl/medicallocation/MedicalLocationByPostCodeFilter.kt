package org.taktik.icure.domain.filter.impl.medicallocation

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.MedicalLocation
import org.taktik.icure.domain.filter.medicallocation.MedicalLocationByPostCodeFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class MedicalLocationByPostCodeFilter(
	override val postCode: String,
	override val desc: String? = null,
) : AbstractFilter<MedicalLocation>, MedicalLocationByPostCodeFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: MedicalLocation, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		item.address?.postalCode == postCode
}