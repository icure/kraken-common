package org.taktik.icure.domain.filter.message

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Message

/**
 * Retrieves all the [Message]s with a delegation for [dataOwnerId], where [Message.tags] contains at least a tag of
 * type [tagType], or if [tagCode] is provided a code with a tag of type [tagType] and code [tagCode]
 */
interface MessageByDataOwnerTagFilter : Filter<String, Message> {
	val dataOwnerId: String
	val tagType: String
	val tagCode: String?
}
