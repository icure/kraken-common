package org.taktik.icure.domain.filter.message

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Message

/**
 * Returns all the [Message]s where [Message.invoiceIds] contains at least one of the provided [invoiceIds].
 * As [Message] is an encryptable entity but this filter does not specify any data owner id, a special permission is
 * needed to use this filter.
 */
interface MessageByInvoiceIdsFilter : Filter<String, Message> {
	val invoiceIds: Set<String>
}
