/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.base.Encryptable
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.asEncryptable

data class ServiceByContactsAndSubcontactsFilter(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val contacts: Set<String>,
	override val subContacts: Set<String>? = null,
	override val startValueDate: Long? = null,
	override val endValueDate: Long? = null
) : AbstractFilter<Service>, org.taktik.icure.domain.filter.service.ServiceByContactsAndSubcontactsFilter {

	override val requiresSecurityPrecondition: Boolean = healthcarePartyId == null
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Service, searchKeyMatcher: (String, Encryptable) -> Boolean): Boolean {
		return (healthcarePartyId == null || item.asEncryptable()?.let { searchKeyMatcher(healthcarePartyId, it) } == true) &&
				contacts.contains(item.contactId) &&
				(subContacts == null || subContacts.intersect((item.subContactIds ?: setOf()).toSet()).isNotEmpty())
	}
}
