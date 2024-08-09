package org.taktik.icure.domain.filter.impl.message

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.message.MessageByParentIdsFilter
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class MessageByParentIdsFilter(
	override val parentIds: List<String>,
	override val desc: String? = null
) : AbstractFilter<Message>, MessageByParentIdsFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Message, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		parentIds.contains(item.parentId)
}