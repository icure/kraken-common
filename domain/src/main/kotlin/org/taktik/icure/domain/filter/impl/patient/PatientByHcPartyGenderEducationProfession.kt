/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.patient

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Gender

data class PatientByHcPartyGenderEducationProfession(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val gender: Gender? = null,
	override val education: String? = null,
	override val profession: String? = null
) : AbstractFilter<Patient>, org.taktik.icure.domain.filter.patient.PatientByHcPartyGenderEducationProfession {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Patient, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return (healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item)) &&
				(gender == null || item.gender != null && item.gender === gender) &&
				(education == null || item.education != null && item.education == education) &&
				(profession == null || item.profession != null && item.profession == profession)
	}
}
