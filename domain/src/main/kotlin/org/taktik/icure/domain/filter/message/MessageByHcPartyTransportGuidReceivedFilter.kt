package org.taktik.icure.domain.filter.message

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Message

/**
 * Retrieves all the [Message.id]s that have a delegation for [healthcarePartyId] and where [Message.transportGuid] is equal
 * to [transportGuid], sorted by [Message.received] in ascending or descending order according to the value of [descending].
 * As this filter explicitly specifies a data owner, it does not require any special permission to be used.
 */
interface MessageByHcPartyTransportGuidReceivedFilter : Filter<String, Message> {
    val healthcarePartyId: String
    val transportGuid: String
    val descending: Boolean?
}