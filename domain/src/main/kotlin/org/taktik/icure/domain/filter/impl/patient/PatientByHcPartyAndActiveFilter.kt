/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.patient

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class PatientByHcPartyAndActiveFilter(
	override val desc: String? = null,
	override val active: Boolean = false,
	override val healthcarePartyId: String? = null,
) : AbstractFilter<Patient>,
	org.taktik.icure.domain.filter.patient.PatientByHcPartyAndActiveFilter {

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Patient, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = (healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item)) && active == item.active
}
