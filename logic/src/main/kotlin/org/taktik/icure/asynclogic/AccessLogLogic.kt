/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.result.AggregatedAccessLogs
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.pagination.PaginatedElement

interface AccessLogLogic : EntityWithSecureDelegationsLogic<AccessLog>, EntityPersister<AccessLog, String> {
	suspend fun createAccessLog(accessLog: AccessLog): AccessLog?
	fun deleteAccessLogs(ids: List<String>): Flow<DocIdentifier>
	fun listAccessLogsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretForeignKeys: List<String>): Flow<AccessLog>
	suspend fun getAccessLog(accessLogId: String): AccessLog?

	/**
	 * Retrieves all the [AccessLog]s which [AccessLog.created] date is between [fromEpoch] and [toEpoch] in a [Flow] for
	 * pagination.
	 *
	 * @param fromEpoch the starting date
	 * @param toEpoch the ending date
	 * @param paginationOffset a [PaginationOffset] for pagination
	 * @param descending whether to sort the elements by [AccessLog.created] descending or ascending.
	 * @return a [Flow] of [PaginatedElement]s.
	 */
	fun listAccessLogsBy(fromEpoch: Long, toEpoch: Long, paginationOffset: PaginationOffset<Long>, descending: Boolean): Flow<PaginatedElement>

	/**
	 * Retrieves all the [AccessLog]s where [AccessLog.user] equals [userId], [AccessLog.accessType] equals [accessType],
	 * if present, and [AccessLog.date] equals [startDate], if present, in a [Flow] for pagination.
	 *
	 * @param userId the id of the user the [AccessLog]s refer to.
	 * @param accessType the type of access to the [AccessLog].
	 * @param startDate only the [AccessLog] which [AccessLog.date] is after this date, if present, will be considered.
	 * @param pagination a [PaginationOffset] for pagination.
	 * @param descending whether the results should be ordered in descending or in ascending order by key.
	 * @return a [Flow] of [PaginatedElement]s
	 */
	fun findAccessLogsByUserAfterDate(userId: String, accessType: String?, startDate: Long?, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<PaginatedElement>
	suspend fun aggregatePatientByAccessLogs(userId: String, accessType: String?, startDate: Long?, startKey: String?, startDocumentId: String?, limit: Int): AggregatedAccessLogs
	fun deleteAccessLogs(ids: Flow<String>): Flow<DocIdentifier>
}
