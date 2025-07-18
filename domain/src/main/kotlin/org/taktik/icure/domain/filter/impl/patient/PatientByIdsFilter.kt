/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.patient

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class PatientByIdsFilter(
	override val ids: Set<String>,
	override val desc: String? = null,
) : AbstractFilter<Patient>,
	Filters.IdsFilter<String, Patient> {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Patient, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = ids.contains(item.id)
}
