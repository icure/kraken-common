package org.taktik.icure.domain.filter.message

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Message
import java.time.Instant

/**
 * Retrieves the ids of all the [Message]s given the [dataOwnerId] (plus all the current access keys if that is
 * equal to the data owner id of the user making the request) and a set of [Message.secretForeignKeys].
 * Only the ids of the Messages where [Message.sent] is not null are returned and the results are sorted by
 * [Message.sent] in ascending or descending order according to the [descending] parameter.
 * As this filter explicitly specifies a data owner, it does not require any special permission to be used.
 */
interface MessageByDataOwnerPatientSentDateFilter : Filter<String, Message> {
	val dataOwnerId: String
	val secretPatientKeys: Set<String>
	val startDate: Instant?
	val endDate: Instant?
	val descending: Boolean?
}