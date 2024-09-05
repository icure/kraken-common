package org.taktik.icure.domain.filter.impl.contact

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.contact.ContactByDataOwnerServiceTagFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class ContactByDataOwnerServiceTagFilter(
	override val dataOwnerId: String,
	override val tagType: String,
	override val tagCode: String?,
	override val desc: String? = null
) : AbstractFilter<Contact>, ContactByDataOwnerServiceTagFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Contact, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) =
		searchKeyMatcher(dataOwnerId, item)
			&& item.services.isNotEmpty()
			&& item.services.any { service ->
				service.tags.any {
					it.type == tagType && (tagCode == null || it.code == tagCode)
				}
			}

}
