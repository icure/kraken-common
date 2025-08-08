package org.taktik.icure.domain.filter.impl.contact

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.contact.ContactByDataOwnerFormIdsFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class ContactByDataOwnerFormIdsFilter(
	override val dataOwnerId: String,
	override val formIds: List<String>,
	override val desc: String? = null,
) : AbstractFilter<Contact>,
	ContactByDataOwnerFormIdsFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Contact, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = searchKeyMatcher(dataOwnerId, item) &&
		(item.subContacts.any { formIds.contains(it.formId) } || item.services.any { formIds.contains(it.formId) })
}
