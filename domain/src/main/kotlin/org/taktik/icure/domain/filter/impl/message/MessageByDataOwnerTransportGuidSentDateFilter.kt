package org.taktik.icure.domain.filter.impl.message

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.message.MessageByDataOwnerTransportGuidSentDateFilter
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.base.HasEncryptionMetadata
import java.time.Instant

data class MessageByDataOwnerTransportGuidSentDateFilter(
	override val dataOwnerId: String,
	override val transportGuid: String,
	override val fromDate: Instant,
	override val toDate: Instant,
	override val descending: Boolean? = null,
	override val desc: String? = null
) : AbstractFilter<Message>, MessageByDataOwnerTransportGuidSentDateFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Message, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		searchKeyMatcher(dataOwnerId, item)
			&& item.transportGuid == transportGuid
			&& item.sent != null && item.sent >= fromDate.toEpochMilli() && item.sent <= toDate.toEpochMilli()
}