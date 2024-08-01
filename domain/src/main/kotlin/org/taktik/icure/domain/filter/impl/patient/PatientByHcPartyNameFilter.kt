/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl.patient

import org.taktik.icure.db.sanitizeString
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.HasEncryptionMetadata
import java.util.*

data class PatientByHcPartyNameFilter(
	override val desc: String? = null,
	override val name: String? = null,
	override val healthcarePartyId: String? = null
) : AbstractFilter<Patient>, org.taktik.icure.domain.filter.patient.PatientByHcPartyNameFilter {

	override val canBeUsedInWebsocket = true
	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Patient, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		val ss = sanitizeString(name)
		return (healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item)) &&
				(sanitizeString(Optional.of<String?>(item.lastName!!).orElse("") + Optional.of<String?>(item.firstName!!).orElse(""))!!.contains(ss!!) ||
						sanitizeString(Optional.of<String?>(item.maidenName!!).orElse(""))!!.contains(ss) ||
						sanitizeString(Optional.of<String?>(item.partnerName!!).orElse(""))!!.contains(ss))
	}
}
