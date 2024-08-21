package org.taktik.icure.domain.filter.timetable

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.TimeTable

/**
 * Retrieves all the [TimeTable] where [TimeTable.agendaId] is equal to [agendaId].
 * As [TimeTable] is an encryptable entity and this filter does not explicitly specify any data owner, a special
 * permission is required to use this filter.
 */
interface TimeTableByAgendaIdFilter : Filter<String, TimeTable> {
	val agendaId: String
}