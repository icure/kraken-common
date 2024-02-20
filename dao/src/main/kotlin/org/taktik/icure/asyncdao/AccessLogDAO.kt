/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.AccessLog

interface AccessLogDAO : GenericDAO<AccessLog> {
	fun listAccessLogsByDate(datastoreInformation: IDatastoreInformation, fromEpoch: Long, toEpoch: Long, paginationOffset: PaginationOffset<Long>, descending: Boolean): Flow<ViewQueryResultEvent>
	fun findAccessLogsByUserAfterDate(datastoreInformation: IDatastoreInformation, userId: String, accessType: String?, startDate: Long?, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>
	fun findAccessLogsByHCPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<AccessLog>

	/**
	 * Finds all the [AccessLog]s for a given search key and secret patient key. All the result will be wrapped in a
	 * [ViewQueryResultEvent] for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and couchdb instance.
	 * @param searchKey the search key.
	 * @param secretPatientKey the secret patient key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the matching [AccessLog]s.
	 */
	fun findAccessLogsBySearchKeyAndSecretPatientKey(datastoreInformation: IDatastoreInformation, searchKey: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>
}
