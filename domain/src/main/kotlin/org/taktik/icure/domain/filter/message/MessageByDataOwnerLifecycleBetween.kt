package org.taktik.icure.domain.filter.message

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Message

/**
 * Retrieves all the [Message]s with a delegation for [dataOwnerId], where the max among [Message.created],
 * [Message.modified], and [Message.deletionDate] is greater or equal than [startTimestamp] (if provided) and
 * less or equal than [endTimestamp] (if provided).
 * This filter explicitly requires a [dataOwnerId], so it does not require any security precondition.
 */
interface MessageByDataOwnerLifecycleBetween : Filter<String, Message> {
	val dataOwnerId: String
	val startTimestamp: Long?
	val endTimestamp: Long?
	val descending: Boolean
}
