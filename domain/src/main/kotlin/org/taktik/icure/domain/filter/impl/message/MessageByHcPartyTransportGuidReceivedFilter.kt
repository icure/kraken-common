package org.taktik.icure.domain.filter.impl.message

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.message.MessageByHcPartyTransportGuidReceivedFilter
import org.taktik.icure.entities.Message

data class MessageByHcPartyTransportGuidReceivedFilter(
	override val healthcarePartyId: String,
	override val transportGuid: String,
	override val descending: Boolean? = null,
	override val desc: String? = null,
) : AbstractFilter<Message>,
	MessageByHcPartyTransportGuidReceivedFilter {

	override val canBeUsedInWebsocket: Boolean = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(healthcarePartyId)

	override fun matches(item: Message, searchKeyMatcher: (String, org.taktik.icure.entities.base.HasEncryptionMetadata) -> Boolean): Boolean = searchKeyMatcher(healthcarePartyId, item) && transportGuid == item.transportGuid
}
