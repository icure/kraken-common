package org.taktik.icure.asynclogic.impl.filter.timetable

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.TimeTableDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.timetable.TimeTableByPeriodAndAgendaIdFilter
import org.taktik.icure.entities.TimeTable

@Service
@Profile("app")
class TimeTableByPeriodAndAgendaIdFilter(
	private val timeTableDAO: TimeTableDAO
) : Filter<String, TimeTable, TimeTableByPeriodAndAgendaIdFilter> {
	override fun resolve(
		filter: TimeTableByPeriodAndAgendaIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = timeTableDAO.listTimeTableIdsByPeriodAndAgendaId(
		datastoreInformation = datastoreInformation,
		agendaId = filter.agendaId,
		startDate = filter.startDate,
		endDate = filter.endDate,
		descending = filter.descending ?: false
	)
}