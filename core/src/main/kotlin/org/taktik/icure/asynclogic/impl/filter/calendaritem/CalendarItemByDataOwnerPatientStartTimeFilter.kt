package org.taktik.icure.asynclogic.impl.filter.calendaritem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CalendarItemDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByDataOwnerPatientStartTimeFilter
import org.taktik.icure.entities.CalendarItem

@Service
@Profile("app")
class CalendarItemByDataOwnerPatientStartTimeFilter(
	private val calendarItemDAO: CalendarItemDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, CalendarItem, CalendarItemByDataOwnerPatientStartTimeFilter> {
	override fun resolve(
		filter: CalendarItemByDataOwnerPatientStartTimeFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		calendarItemDAO
			.listCalendarItemIdsByDataOwnerPatientStartTime(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
				secretForeignKeys = filter.secretPatientIds,
				startDate = filter.startDate,
				endDate = filter.endDate,
				descending = filter.descending ?: false,
			).also {
				emitAll(it)
			}
	}
}
