package org.taktik.icure.domain.filter.accesslog

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.AccessLog
import java.time.Instant

/**
 * Retrieves all the [AccessLog]s where [AccessLog.user], [AccessLog.accessType], and [AccessLog.date] are not null,
 * [AccessLog.user] is equal to [userId], [AccessLog.accessType] is equal to [accessType] and [AccessLog.date] is
 * greater than or equal to [startDate].
 * If [accessType] is null, then [startDate] will have no effect on the results.
 * The retrieved [AccessLog]s will be sorted by [AccessLog.accessType] and [AccessLog.date] in ascending or descending
 * order according to the value of the [descending] parameter.
 */
interface AccessLogByUserIdUserTypeDateFilter : Filter<String, AccessLog> {
	val userId: String
	val accessType: String?
	val startDate: Instant?
	val descending: Boolean?
}