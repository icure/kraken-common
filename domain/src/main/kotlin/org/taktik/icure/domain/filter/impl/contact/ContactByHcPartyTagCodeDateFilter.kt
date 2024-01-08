/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.contact

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.Encryptable
import org.taktik.icure.entities.base.containsStubWithTypeAndCode

data class ContactByHcPartyTagCodeDateFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val tagType: String? = null,
	override val tagCode: String? = null,
	override val codeType: String? = null,
	override val codeCode: String? = null,
	override val startOfContactOpeningDate: Long? = null,
	override val endOfContactOpeningDate: Long? = null
) : AbstractFilter<Contact>, org.taktik.icure.domain.filter.contact.ContactByHcPartyTagCodeDateFilter {
	init {
		if (tagCode != null) {
			require(tagType != null) { "If you specify tagCode you must also specify tagType" }
		}
		if (codeCode != null) {
			require(codeType != null) { "If you specify codeCode you must also specify codeType" }
		}
	}

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Contact, searchKeyMatcher: (String, Encryptable) -> Boolean): Boolean {
		return (
			(healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item)) &&
			item.services.any { svc -> // Search on service fields instead of contact fields is intentional
				(tagType == null || svc.tags.containsStubWithTypeAndCode(tagType, tagCode)) &&
				(codeType == null || svc.codes.containsStubWithTypeAndCode(codeType, codeCode)) &&
				(startOfContactOpeningDate == null || svc.valueDate != null && svc.valueDate > startOfContactOpeningDate || svc.openingDate != null && svc.openingDate > startOfContactOpeningDate) &&
				(endOfContactOpeningDate == null || svc.valueDate != null && svc.valueDate < endOfContactOpeningDate || svc.openingDate != null && svc.openingDate < endOfContactOpeningDate)
			}
		)
	}
}
