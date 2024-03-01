/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Place

interface PlaceDAO : GenericDAO<Place> {

	/**
	 * Retrieves all the [Place]s in a group in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Place]s.
	 */
	fun getAllPlaces(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<Nothing>): Flow<ViewQueryResultEvent>

}
