package org.taktik.icure.domain.filter.impl.message

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.message.MessageByInvoiceIdsFilter
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class MessageByInvoiceIdsFilter(
	override val invoiceIds: Set<String>,
	override val desc: String? = null
) : AbstractFilter<Message>, MessageByInvoiceIdsFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Message, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		invoiceIds.intersect(item.invoiceIds).isNotEmpty()
}