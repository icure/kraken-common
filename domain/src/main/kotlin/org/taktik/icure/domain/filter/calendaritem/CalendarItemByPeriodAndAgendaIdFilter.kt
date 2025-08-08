package org.taktik.icure.domain.filter.calendaritem

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.CalendarItem

/**
 * Retrieves all the [CalendarItem]s where [CalendarItem.agendaId] is equal to [agendaId] and [CalendarItem.startTime]
 * is between the [startTime]and [endTime].
 * Both [startTime] and [endTime] are fuzzy dates in the YYYYMMDDHHMMSS format.
 * The resulting entities will be sorted by agendaId, startTime in ascending or descending order according to the value
 * of [descending].
 * As [CalendarItem] is an encryptable entity but this filter does not require any data owner id, it requires a security
 * precondition.
 */
interface CalendarItemByPeriodAndAgendaIdFilter : Filter<String, CalendarItem> {
	val agendaId: String
	val startTime: Long
	val endTime: Long
	val descending: Boolean?
}
