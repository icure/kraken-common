package org.taktik.icure.domain.filter.message

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Message

/**
 * Retrieves all the [Message]s with a delegation for [dataOwnerId], and where [Message.fromAddress] is equal to [fromAddress].
 * As this filter explicitly specifies a data owner, it does not require any special permission to be used.
 */
interface MessageByDataOwnerFromAddressFilter : Filter<String, Message> {
	val dataOwnerId: String
	val fromAddress: String
}
