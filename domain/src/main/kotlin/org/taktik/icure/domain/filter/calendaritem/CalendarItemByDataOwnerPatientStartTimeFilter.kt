package org.taktik.icure.domain.filter.calendaritem

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.CalendarItem

/**
 * Retrieves all the [CalendarItem]s that the data owner with id [dataOwnerId] can access, where [CalendarItem.secretForeignKeys]
 * contains at least one of [secretPatientIds] and where [CalendarItem.startTime] is greater than or
 * equal to [startDate] (if provided) and less than or equal to [endDate] (if provided).
 * If [dataOwnerId] is the data owner making the request, then also the available secret access keys will be used to
 * retrieve the results.
 * If [startDate] is null, all the [CalendarItem]s since the beginning of time will be returned.
 * If [endDate] is null, all the [CalendarItem]s until the end of time will be returned.
 * The results will be sorted by [CalendarItem.startTime] in ascending or descending order according to the value of the
 * [descending] parameter.
 * This filter explicitly requires a [dataOwnerId], so it does not require any security precondition.
 */
interface CalendarItemByDataOwnerPatientStartTimeFilter : Filter<String, CalendarItem> {
	val dataOwnerId: String
	val startDate: Long?
	val endDate: Long?
	val secretPatientIds: Set<String>
	val descending: Boolean?
}
