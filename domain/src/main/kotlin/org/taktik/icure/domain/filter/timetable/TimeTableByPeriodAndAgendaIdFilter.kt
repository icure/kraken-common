package org.taktik.icure.domain.filter.timetable

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.TimeTable

/**
 * Retrieves the [TimeTable]s where the [TimeTable.startTime] to [TimeTable.endTime] interval
 * intersects with the [startDate] fuzzy date to [endDate] fuzzy date interval, and where [TimeTable.agendaId] is
 * equal to [agendaId].
 * If [TimeTable.startTime] is null, then it will be considered as always intersecting the start of the interval.
 * If [TimeTable.endTime] is null, then it will be considered as always intersecting the end of the interval.
 * If [startDate] is null, then the interval will start at 0.
 * If [endDate] is null, then the interval will end at an arbitrary big number.
 * The results will be sorted by [TimeTable.startTime] in descending or ascending order according to the [descending]
 * parameter.
 * As [TimeTable] is an encryptable entity and this filter does not explicitly specify any data owner, a special
 * permission is required to use this filter.
 */
interface TimeTableByPeriodAndAgendaIdFilter : Filter<String, TimeTable> {
	val agendaId: String
	val startDate: Long?
	val endDate: Long?
	val descending: Boolean?
}
