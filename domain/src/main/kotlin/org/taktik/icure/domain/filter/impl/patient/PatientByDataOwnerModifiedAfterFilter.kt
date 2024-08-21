package org.taktik.icure.domain.filter.impl.patient

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.patient.PatientByDataOwnerModifiedAfterFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class PatientByDataOwnerModifiedAfterFilter(
	override val dataOwnerId: String,
	override val startDate: Long?,
	override val endDate: Long?,
	override val descending: Boolean?,
	override val desc: String?
)  : AbstractFilter<Patient>, PatientByDataOwnerModifiedAfterFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Patient, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		val lastModified = item.modified ?: item.created
		return searchKeyMatcher(dataOwnerId, item)
			&& lastModified != null
			&& (startDate == null || lastModified <= startDate)
			&& (endDate == null || lastModified >= endDate)
	}

}
