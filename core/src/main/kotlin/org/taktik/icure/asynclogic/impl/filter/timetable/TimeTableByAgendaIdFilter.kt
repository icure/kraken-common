package org.taktik.icure.asynclogic.impl.filter.timetable

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.TimeTableDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.timetable.TimeTableByAgendaIdFilter
import org.taktik.icure.entities.TimeTable

@Service
@Profile("app")
class TimeTableByAgendaIdFilter(
	private val timeTableDAO: TimeTableDAO
) : Filter<String, TimeTable, TimeTableByAgendaIdFilter> {

	override fun resolve(
		filter: TimeTableByAgendaIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = timeTableDAO.listTimeTableIdsByAgendaId(
		datastoreInformation = datastoreInformation,
		agendaId = filter.agendaId
	)
}
