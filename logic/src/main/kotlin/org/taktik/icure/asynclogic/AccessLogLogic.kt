/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.result.AggregatedAccessLogs
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.pagination.PaginationElement

interface AccessLogLogic :
	EntityWithSecureDelegationsLogic<AccessLog>,
	EntityPersister<AccessLog> {
	suspend fun createAccessLog(accessLog: AccessLog): AccessLog

	/**
	 * Retrieves the all the [AccessLog]s given the [hcPartyId] (and its access keys if it is the current user making
	 * the request) and a set of [AccessLog.secretForeignKeys].
	 *
	 * @param hcPartyId the id of the Data Owner allowed to access the [AccessLog]s.
	 * @param secretForeignKeys a [List] of [AccessLog.secretForeignKeys].
	 * @return a [Flow] of [AccessLog]s.
	 */
	@Deprecated("This method is inefficient for high volumes of keys, use listAccessLogIdsByDataOwnerPatientDate instead")
	fun listAccessLogsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretForeignKeys: List<String>): Flow<AccessLog>

	/**
	 * Retrieves the ids of all the [AccessLog]s given the [dataOwnerId] (and its access keys if it is the current
	 * user making the request) and a set of [AccessLog.secretForeignKeys].
	 * Only the ids of the Access Logs where [AccessLog.date] is not null are returned and the results are sorted by
	 * [AccessLog.date] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param dataOwnerId the id of a data owner.
	 * @param secretForeignKeys a [Set] of [AccessLog.secretForeignKeys].
	 * @param startDate if not null, only the ids of the Access Logs where [AccessLog.date] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate if not null, only the ids of the Access Logs where [AccessLog.date] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [AccessLog.date] ascending or descending.
	 * @return a [Flow] of Access Log ids.
	 */
	fun listAccessLogIdsByDataOwnerPatientDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
	suspend fun getAccessLog(accessLogId: String): AccessLog?

	/**
	 * Retrieves all the [AccessLog]s with the provided [ids].
	 *
	 * @param ids the ids of the [AccessLog] to retrieve.
	 * @return a [Flow] of [AccessLog].
	 */
	fun getAccessLogs(ids: List<String>): Flow<AccessLog>

	/**
	 * Retrieves all the [AccessLog]s which [AccessLog.date] date is between [fromEpoch] and [toEpoch] in a [Flow] for
	 * pagination.
	 *
	 * @param fromEpoch the starting date
	 * @param toEpoch the ending date
	 * @param paginationOffset a [PaginationOffset] for pagination
	 * @param descending whether to sort the elements by [AccessLog.created] descending or ascending.
	 * @return a [Flow] of [PaginationElement]s.
	 */
	fun listAccessLogsBy(fromEpoch: Long, toEpoch: Long, paginationOffset: PaginationOffset<Long>, descending: Boolean): Flow<PaginationElement>

	/**
	 * Retrieves all the [AccessLog]s where [AccessLog.user] equals [userId], [AccessLog.accessType] equals [accessType],
	 * if present, and [AccessLog.date] is greater than or equal to [startDate], if present, in a [Flow] for pagination.
	 *
	 * @param userId the id of the user the [AccessLog]s refer to.
	 * @param accessType the type of access to the [AccessLog].
	 * @param startDate only the [AccessLog] which [AccessLog.date] is after this date, if present, will be considered.
	 * @param pagination a [PaginationOffset] for pagination.
	 * @param descending whether the results should be ordered in descending or in ascending order by key.
	 * @return a [Flow] of [PaginationElement]s
	 */
	fun findAccessLogsByUserAfterDate(userId: String, accessType: String?, startDate: Long?, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<PaginationElement>
	suspend fun aggregatePatientByAccessLogs(userId: String, accessType: String?, startDate: Long?, startKey: String?, startDocumentId: String?, limit: Int): AggregatedAccessLogs
}
