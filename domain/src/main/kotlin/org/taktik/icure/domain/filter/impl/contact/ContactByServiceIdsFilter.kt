/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.contact

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class ContactByServiceIdsFilter(
	override val desc: String? = null,
	override val ids: List<String>? = null,
) : AbstractFilter<Contact>,
	org.taktik.icure.domain.filter.contact.ContactByServiceIdsFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Contact, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.services.stream().filter { (id) -> ids!!.contains(id) }.findAny().isPresent
}
