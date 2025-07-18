/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl.healthelement

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.containsStubWithTypeAndCode

data class HealthElementByHcPartyTagCodeFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String,
	override val codeType: String? = null,
	override val codeCode: String? = null,
	override val tagType: String? = null,
	override val tagCode: String? = null,
	override val status: Int? = null,
) : AbstractFilter<HealthElement>,
	org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyTagCodeFilter {
	init {
		if (tagCode != null) {
			require(tagType != null) { "If you specify tagCode you must also specify tagType" }
		}
		if (codeCode != null) {
			require(codeType != null) { "If you specify codeCode you must also specify codeType" }
		}
	}

	override val canBeUsedInWebsocket = true

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(healthcarePartyId)

	override fun matches(item: HealthElement, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = (
		(searchKeyMatcher(healthcarePartyId, item)) &&
			(codeType == null || item.codes.containsStubWithTypeAndCode(codeType, codeCode)) &&
			(tagType == null || item.tags.containsStubWithTypeAndCode(tagType, tagCode)) &&
			(status == null || item.status == status)
		)
}
