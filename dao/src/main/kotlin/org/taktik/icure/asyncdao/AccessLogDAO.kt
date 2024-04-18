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

	/**
	 * Retrieves the all the [AccessLog]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of
	 * [AccessLog.secretForeignKeys].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner iId + access keys).
	 * @param secretPatientKeys a [List] of [AccessLog.secretForeignKeys].
	 * @return a [Flow] of [AccessLog]s.
	 */
	fun findAccessLogsByHCPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<AccessLog>

	/**
	 * Retrieves the ids of all the [AccessLog]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of
	 * [AccessLog.secretForeignKeys].
	 * Only the ids of the Access Logs where [AccessLog.date] is not null are returned and the results are sorted by
	 * [AccessLog.date] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [AccessLog.secretForeignKeys].
	 * @param startDate a timestamp. If not null, only the ids of the Access Logs where [AccessLog.date] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a timestamp. If not null, only the ids of the Access Logs where [AccessLog.date] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [AccessLog.date] ascending or descending.
	 * @return a [Flow] of Access Log ids.
	 */
	fun findAccessLogIdsByDataOwnerPatientDate(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
}
