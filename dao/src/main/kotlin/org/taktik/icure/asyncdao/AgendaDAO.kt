/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Agenda

interface AgendaDAO : GenericDAO<Agenda> {

	/**
	 * Retrieves all the [Agenda]s in a group in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param offset a [PaginationOffset] of [Nothing] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Agenda]s.
	 */
	fun getAllPaginated(datastoreInformation: IDatastoreInformation, offset: PaginationOffset<Nothing>): Flow<ViewQueryResultEvent>
	fun getAgendasByUser(datastoreInformation: IDatastoreInformation, userId: String): Flow<Agenda>
	fun getReadableAgendaByUser(datastoreInformation: IDatastoreInformation, userId: String): Flow<Agenda>
}
