package org.taktik.icure.asynclogic.impl.filter.calendaritem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CalendarItemDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByPeriodAndAgendaIdFilter
import org.taktik.icure.entities.CalendarItem

@Service
@Profile("app")
class CalendarItemByPeriodAndAgendaIdFilter(
	private val calendarItemDAO: CalendarItemDAO,
) : Filter<String, CalendarItem, CalendarItemByPeriodAndAgendaIdFilter> {
	override fun resolve(
		filter: CalendarItemByPeriodAndAgendaIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = calendarItemDAO
		.listCalendarItemByPeriodAndAgendaId(
			datastoreInformation = datastoreInformation,
			startDate = filter.startTime,
			endDate = filter.endTime,
			agendaId = filter.agendaId,
			descending = filter.descending ?: false,
		).map { it.id }
}
