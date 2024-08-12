package org.taktik.icure.domain.filter.accesslog

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.AccessLog
import java.time.Instant

/**
 * Retrieves all the [AccessLog]s wth a delegation for [dataOwnerId], where [AccessLog.secretForeignKeys]
 * contains at least one of [secretPatientIds] and where [AccessLog.date] is greater than or
 * equal to [startDate] (if provided) and less than or equal to [endDate] (if provided).
 * If [startDate] is null, all the [AccessLog]s since the beginning of time will be returned.
 * If [endDate] is null, all the [AccessLog]s until the end of time will be returned.
 * The results will be sorted by [AccessLog.date] in ascending or descending order according to the value of the
 * [descending] parameter.
 * This filter explicitly requires a [dataOwnerId], so it does not require any security precondition.
 */
interface AccessLogByDataOwnerPatientDateFilter : Filter<String, AccessLog> {
	val dataOwnerId: String
	val startDate: Instant?
	val endDate: Instant?
	val secretPatientIds: Set<String>
	val descending: Boolean?
}
