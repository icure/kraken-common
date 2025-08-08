package org.taktik.icure.domain.filter.impl.contact

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.contact.ContactByDataOwnerPatientOpeningDateFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class ContactByDataOwnerPatientOpeningDateFilter(
	override val dataOwnerId: String,
	override val secretForeignKeys: Set<String>,
	override val startDate: Long? = null,
	override val endDate: Long? = null,
	override val descending: Boolean? = null,
	override val desc: String? = null,
) : AbstractFilter<Contact>,
	ContactByDataOwnerPatientOpeningDateFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Contact, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = searchKeyMatcher(dataOwnerId, item) &&
		item.secretForeignKeys.intersect(secretForeignKeys).isNotEmpty() &&
		(startDate == null || (item.openingDate != null && item.openingDate >= startDate)) &&
		(endDate == null || (item.openingDate != null && item.openingDate <= endDate))
}
