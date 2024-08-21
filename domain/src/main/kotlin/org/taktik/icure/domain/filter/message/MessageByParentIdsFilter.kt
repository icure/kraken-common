package org.taktik.icure.domain.filter.message

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Message

/**
 * Returns all the [Message]s where [Message.parentId] is among the provided [parentIds].
 * As [Message] is an encryptable entity but this filter does not specify any data owner id, a special permission is
 * needed to use this filter.
 */
interface MessageByParentIdsFilter : Filter<String, Message> {
	val parentIds: List<String>
}