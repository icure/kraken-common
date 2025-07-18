/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.patient

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.patient.PatientByDataOwnerTagFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.containsStubWithTypeAndCode

data class PatientByDataOwnerTagFilter(
	override val desc: String? = null,
	override val dataOwnerId: String,
	override val tagType: String,
	override val tagCode: String? = null,
) : AbstractFilter<Patient>,
	PatientByDataOwnerTagFilter {

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Patient, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = (searchKeyMatcher(dataOwnerId, item)) &&
		(item.tags.containsStubWithTypeAndCode(tagType, tagCode))
}
