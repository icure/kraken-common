package org.taktik.icure.domain.filter.impl.message

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.message.MessageByDataOwnerTagFilter
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.base.HasEncryptionMetadata

class MessageByDataOwnerTagFilter(
	override val dataOwnerId: String,
	override val tagType: String,
	override val tagCode: String?,
	override val desc: String?,
) : AbstractFilter<Message>,
	MessageByDataOwnerTagFilter {
	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(
		item: Message,
		searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean
	): Boolean =
		searchKeyMatcher(dataOwnerId, item) &&
			if (tagCode != null)
				item.tags.any { it.type == tagType && it.code == tagCode }
			else
				item.tags.any { it.type == tagType }
}

