package org.taktik.icure.asynclogic.impl.filter.calendaritem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CalendarItemDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByPeriodAndDataOwnerIdFilter
import org.taktik.icure.entities.CalendarItem

@Service
@Profile("app")
class CalendarItemByPeriodAndDataOwnerIdFilter(
	private val calendarItemDAO: CalendarItemDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, CalendarItem, CalendarItemByPeriodAndDataOwnerIdFilter> {
	override fun resolve(
		filter: CalendarItemByPeriodAndDataOwnerIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		val result = LinkedHashSet<String>()
		sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId).forEach { key ->
			result.addAll(
				calendarItemDAO
					.listCalendarItemIdsByPeriodAndDataOwnerId(
						datastoreInformation = datastoreInformation,
						dataOwnerId = key,
						startDate = filter.startTime,
						endDate = filter.endTime,
					).toSet(),
			)
		}
		emitAll(result.asFlow())
	}
}
