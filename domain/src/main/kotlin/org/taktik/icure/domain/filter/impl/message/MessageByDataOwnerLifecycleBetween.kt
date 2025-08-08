package org.taktik.icure.domain.filter.impl.message

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.message.MessageByDataOwnerLifecycleBetween
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class MessageByDataOwnerLifecycleBetween(
	override val dataOwnerId: String,
	override val startTimestamp: Long?,
	override val endTimestamp: Long?,
	override val descending: Boolean,
	override val desc: String? = null,
) : AbstractFilter<Message>,
	MessageByDataOwnerLifecycleBetween {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: Message, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = searchKeyMatcher(dataOwnerId, item) &&
		(item.created != null || item.modified != null || item.deletionDate != null) &&
		listOfNotNull(item.created, item.modified, item.deletionDate).max().let {
			(startTimestamp == null || it >= startTimestamp) && (endTimestamp == null || it <= endTimestamp)
		}
}
