package org.taktik.icure.domain.filter.impl.message

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.message.MessageByDataOwnerCodeFilter
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.base.HasEncryptionMetadata

class MessageByDataOwnerCodeFilter(
	override val dataOwnerId: String,
	override val codeType: String,
	override val codeCode: String?,
	override val desc: String? = null,
) : AbstractFilter<Message>,
	MessageByDataOwnerCodeFilter {
	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(
		item: Message,
		searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean
	): Boolean =
		searchKeyMatcher(dataOwnerId, item) &&
			if (codeCode != null)
				item.codes.any { it.type == codeType && it.code == codeCode }
			else
				item.codes.any { it.type == codeType }
}
