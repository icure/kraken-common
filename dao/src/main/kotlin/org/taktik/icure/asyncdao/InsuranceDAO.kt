/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Insurance

interface InsuranceDAO : GenericDAO<Insurance> {
	fun listInsurancesByCode(datastoreInformation: IDatastoreInformation, code: String): Flow<Insurance>

	fun listInsurancesByName(datastoreInformation: IDatastoreInformation, name: String): Flow<Insurance>

	/**
	 * Retrieves all the insurances in the group specified in the [IDatastoreInformation] in a format
	 * for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to specify group and CouchDB instance.
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for the pagination.
	 * @return a [Flow] of [Insurance]s wrapped in [ViewQueryResultEvent]s for pagination.
	 */
	fun getAllInsurances(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<Nothing>): Flow<ViewQueryResultEvent>
}
