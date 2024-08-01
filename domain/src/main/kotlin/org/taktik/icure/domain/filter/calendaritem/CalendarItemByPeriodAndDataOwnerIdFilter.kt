package org.taktik.icure.domain.filter.calendaritem

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.CalendarItem

/**
 * Retrieves all the [CalendarItem]s that the data owner with id [dataOwnerId] can access and where [CalendarItem.startTime]
 * is between [startTime] and [endTime] or [CalendarItem.endTime] is between that same interval.
 * Both [startTime] and [endTime] are fuzzy dates in the YYYYMMDDHHMMSS format.
 * As the result comes from different views, the result of this filtered is not sorted.
 * This filter requires a data owner id, so it does not need a security precondition.
 */
interface CalendarItemByPeriodAndDataOwnerIdFilter : Filter<String, CalendarItem> {
	val dataOwnerId: String
	val startTime: Long
	val endTime: Long
}