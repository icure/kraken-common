package org.taktik.icure.domain.filter.impl.patient

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.Encryptable
import org.taktik.icure.entities.embed.Identifier

data class PatientByHcPartyAndIdentifiersFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val identifiers: List<Identifier> = emptyList()
) : AbstractFilter<Patient>, org.taktik.icure.domain.filter.patient.PatientByHcPartyAndIdentifiersFilter {

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()
	override fun matches(item: Patient, searchKeyMatcher: (String, Encryptable) -> Boolean): Boolean {
		return item.endOfLife == null
				&& (healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item))
				&& identifiers.any { searchIdentifier -> item.identifier.any { it.system == searchIdentifier.system && it.id == searchIdentifier.id } }
	}
}
