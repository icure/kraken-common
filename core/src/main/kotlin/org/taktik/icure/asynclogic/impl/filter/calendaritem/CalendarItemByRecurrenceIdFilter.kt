package org.taktik.icure.asynclogic.impl.filter.calendaritem

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CalendarItemDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByRecurrenceIdFilter
import org.taktik.icure.entities.CalendarItem

@Service
@Profile("app")
class CalendarItemByRecurrenceIdFilter(
	val calendarItemDAO: CalendarItemDAO
) : Filter<String, CalendarItem, CalendarItemByRecurrenceIdFilter> {

	override fun resolve(
		filter: CalendarItemByRecurrenceIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = calendarItemDAO.listCalendarItemIdsByRecurrenceId(
		datastoreInformation = datastoreInformation,
		recurrenceId = filter.recurrenceId
	)

}