package org.taktik.icure.domain.filter.accesslog

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.AccessLog
import java.time.Instant

/**
 * Retrieves all the [AccessLog]s where [AccessLog.date] is not null, [AccessLog.date] is greater than or
 * equal to [startDate] (if provided) and less than or equal to [endDate] (if provided).
 * If [startDate] is null, then it will be considered as the lowest possible timestamp.
 * If [endDate] is null, all the [AccessLog]s until the end of time will be returned.
 * The retrieved [AccessLog]s will be sorted by [AccessLog.date] in ascending order by default. It is possible to
 * return the [AccessLog]s in descending order by setting [descending] to true.
 * As [AccessLog] is an encryptable entity but this filter does not require any data owner id, it requires a security
 * precondition.
 */
interface AccessLogByDateFilter : Filter<String, AccessLog> {
	val startDate: Instant?
	val endDate: Instant?
	val descending: Boolean?
}