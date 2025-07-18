/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.patient

import org.taktik.icure.db.sanitizeString
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class PatientByHcPartyNameContainsFuzzyFilter(
	override val desc: String? = null,
	override val searchString: String? = null,
	override val healthcarePartyId: String? = null,
) : AbstractFilter<Patient>,
	org.taktik.icure.domain.filter.patient.PatientByHcPartyNameContainsFuzzyFilter {

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Patient, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		val ss = sanitizeString(searchString)
		return healthcarePartyId?.let { searchKeyMatcher(it, item) } != false &&
			(
				ss?.let {
					(
						(item.lastName?.let { sanitizeString(it) } ?: "") +
							(
								item.firstName?.let { sanitizeString(it) }
									?: ""
								)
						).contains(ss) ||
						(item.maidenName?.let { sanitizeString(it) } ?: "").contains(ss) ||
						(item.partnerName?.let { sanitizeString(it) } ?: "").contains(ss)
				} != false
				)
	}
}
