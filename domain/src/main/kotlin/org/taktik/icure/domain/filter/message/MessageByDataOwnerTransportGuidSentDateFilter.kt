package org.taktik.icure.domain.filter.message

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Message
import java.time.Instant

/**
 * Retrieves all [Message.id]s with a delegation for [dataOwnerId], with the provided [Message.transportGuid] and where
 * [Message.sent] date is between [fromDate] and [toDate], sorted by [Message.sent] in ascending or descending order
 * according to the value of the [descending] parameter.
 * As this filter explicitly specifies a data owner, it does not require any special permission to be used.
 */
interface MessageByDataOwnerTransportGuidSentDateFilter : Filter<String, Message> {
	val dataOwnerId: String
	val transportGuid: String
	val fromDate: Instant?
	val toDate: Instant?
	val descending: Boolean?
}