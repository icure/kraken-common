/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItemType

interface CalendarItemTypeDAO : GenericDAO<CalendarItemType> {

	fun listCalendarItemTypesByAgendaId(datastoreInformation: IDatastoreInformation, agendaId: String): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [CalendarItemType]s in a group in a format for pagination, including all the entities where
	 * [CalendarItemType.deletionDate] is not null.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent]s containing the [CalendarItemType]s.
	 */
	fun getCalendarItemsWithDeleted(datastoreInformation: IDatastoreInformation, offset: PaginationOffset<String>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [CalendarItemType]s in a group, including all the entities where
	 * [CalendarItemType.deletionDate] is not null.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @return a [Flow] of [CalendarItemType]s.
	 */
	fun getCalendarItemsWithDeleted(datastoreInformation: IDatastoreInformation): Flow<CalendarItemType>
}
