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
import org.taktik.icure.pagination.PaginationElement

interface AccessLogDAO : GenericDAO<AccessLog> {

	/**
	 * Retrieves all the [AccessLog]s where [AccessLog.date] is not null, [AccessLog.date] is greater than or
	 * equal to [fromEpoch] and less than or equal to [toEpoch] in a format for pagination.
	 * The retrieved [AccessLog]s will be sorted by [AccessLog.date] in ascending order by default. It is possible to
	 * return the [AccessLog]s in descending order by setting [descending] to true.
	 *
	 *  @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 *  @param fromEpoch the lower bound timestamp.
	 *  @param toEpoch the upper bound timestamp.
	 *  @param paginationOffset a [PaginationOffset] of [Long] to iterate over the pages.
	 *  @param descending whether to return the result sorted by [AccessLog.date] in descending order.
	 *  @return a [Flow] of [ViewQueryResultEvent] containing the matched [AccessLog]s.
	 */
	fun listAccessLogsByDate(datastoreInformation: IDatastoreInformation, fromEpoch: Long, toEpoch: Long, paginationOffset: PaginationOffset<Long>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [AccessLog.id] where [AccessLog.date] is not null, [AccessLog.date] is greater than or
	 * equal to [fromEpoch] and less than or equal to [toEpoch].
	 * The retrieved ids will be sorted by [AccessLog.date] in ascending order by default. It is possible to
	 * return the [AccessLog]s in descending order by setting [descending] to true.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param fromEpoch the lower bound timestamp.
	 * @param toEpoch the upper bound timestamp.
	 * @param descending whether to return the result sorted by [AccessLog.date] in descending order.
	 * @return a [Flow] of [AccessLog.id]s.
	 */
	fun listAccessLogIdsByDate(datastoreInformation: IDatastoreInformation, fromEpoch: Long, toEpoch: Long, descending: Boolean): Flow<String>

	/**
	 * Retrieves all the [AccessLog]s where [AccessLog.user] equals [userId], [AccessLog.accessType] equals [accessType],
	 * if present, and [AccessLog.date] is greater than or equal to [startDate], if present, in a [Flow] for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param userId the id of the user the [AccessLog]s refer to.
	 * @param accessType the type of access to the [AccessLog].
	 * @param startDate only the [AccessLog] which [AccessLog.date] is after this date, if present, will be considered.
	 * @param pagination a [PaginationOffset] for pagination.
	 * @param descending whether the results should be ordered in descending or in ascending order by key.
	 * @return a [Flow] of [PaginationElement]s
	 */
	fun findAccessLogsByUserAfterDate(datastoreInformation: IDatastoreInformation, userId: String, accessType: String?, startDate: Long?, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [AccessLog.id]s where [AccessLog.user] equals [userId], [AccessLog.accessType] equals [accessType],
	 * if present, and [AccessLog.date] is greater than or equal to [startDate], if present.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param userId the id of the user the [AccessLog]s refer to.
	 * @param accessType the type of access to the [AccessLog].
	 * @param startDate only the [AccessLog] which [AccessLog.date] is after this date, if present, will be considered.
	 * @param descending whether the results should be ordered in descending or in ascending order by key.
	 * @return a [Flow] of [AccessLog.id]s
	 */
	fun listAccessLogIdsByUserAfterDate(datastoreInformation: IDatastoreInformation, userId: String, accessType: String?, startDate: Long?, descending: Boolean): Flow<String>

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
	@Deprecated("This method is inefficient for high volumes of keys, use listAccessLogIdsByDataOwnerPatientDate instead")
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
	fun listAccessLogIdsByDataOwnerPatientDate(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
}
