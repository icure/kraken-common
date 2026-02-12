package org.taktik.icure.domain.filter.message

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Message

/**
 * Retrieves all the [Message]s with a delegation for [dataOwnerId], where [Message.codes] contains at least a code of
 * type [codeType], or if [codeCode] is provided a code with a code of type [codeType] and code [codeCode]
 */
interface MessageByDataOwnerCodeFilter : Filter<String, Message> {
	val dataOwnerId: String
	val codeType: String
	val codeCode: String?
}
