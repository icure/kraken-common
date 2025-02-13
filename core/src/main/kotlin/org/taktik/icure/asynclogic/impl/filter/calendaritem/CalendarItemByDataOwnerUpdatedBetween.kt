package org.taktik.icure.asynclogic.impl.filter.calendaritem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.CalendarItemDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByDataOwnerUpdatedBetween
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys

@Service
@Profile("app")
class CalendarItemByDataOwnerUpdatedBetween(
	private val calendarItemDAO: CalendarItemDAO,
	private val sessionInformationProvider: SessionInformationProvider
) : Filter<String, CalendarItem, CalendarItemByDataOwnerUpdatedBetween> {

	override fun resolve(
		filter: CalendarItemByDataOwnerUpdatedBetween,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		mergeUniqueIdsForSearchKeys(sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId)) { key ->
			calendarItemDAO.listCalendarItemIdsByDataOwnerUpdatedBetween(
				datastoreInformation = datastoreInformation,
				searchKey = key,
				updatedAfter = filter.updatedAfter,
				updatedBefore = filter.updatedBefore,
				descending = filter.descending
			)
		}.let { emitAll(it) }
	}

}