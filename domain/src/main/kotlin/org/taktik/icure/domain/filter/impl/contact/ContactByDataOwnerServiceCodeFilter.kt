package org.taktik.icure.domain.filter.impl.contact

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.contact.ContactByDataOwnerServiceCodeFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class ContactByDataOwnerServiceCodeFilter(
	override val dataOwnerId: String,
	override val codeType: String,
	override val codeCode: String?,
	override val desc: String? = null,
) : AbstractFilter<Contact>,
	ContactByDataOwnerServiceCodeFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Contact, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = searchKeyMatcher(dataOwnerId, item) &&
		item.services.isNotEmpty() &&
		item.services.any { service ->
			service.codes.any {
				it.type == codeType && (codeCode == null || it.code == codeCode)
			}
		}
}
