package org.taktik.icure.domain.filter.accesslog

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.AccessLog
import java.time.Instant

/**
 * Retrieves all the [AccessLog]s where [AccessLog.date] is not null, [AccessLog.date] is greater than or
 * equal to [startDate] (if provided) and less than or equal to [endDate] (if provided).
 * If [startDate] is null, all the [AccessLog]s since the beginning of time will be returned.
 * If [endDate] is null, all the [AccessLog]s until the end of time will be returned.
 * The retrieved [AccessLog]s will be sorted by [AccessLog.date] in ascending order by default. It is possible to
 * return the [AccessLog]s in descending order by setting [descending] to true.
 */
interface AccessLogByDateFilter : Filter<String, AccessLog> {
	val startDate: Instant?
	val endDate: Instant?
	val descending: Boolean?
}