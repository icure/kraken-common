package org.taktik.icure.domain.filter.impl.message

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.message.MessageByDataOwnerToAddressFilter
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class MessageByDataOwnerToAddressFilter(
	override val dataOwnerId: String,
	override val toAddress: String,
	override val desc: String? = null,
) : AbstractFilter<Message>, MessageByDataOwnerToAddressFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Message, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		searchKeyMatcher(dataOwnerId, item) && item.toAddresses.contains(toAddress)
}