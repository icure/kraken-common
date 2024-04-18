/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.AccessLogLogic
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.result.AggregatedAccessLogs
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface AccessLogService : EntityWithSecureDelegationsService<AccessLog> {
	suspend fun createAccessLog(accessLog: AccessLog): AccessLog?

	/**
	 * Deletes a batch of [AccessLog]s.
	 * If the user does not have the permission to delete an [AccessLog] or the [AccessLog] does not exist, then it
	 * is ignored without any warning.
	 *
	 * @param ids a [List] containing the ids of the [AccessLog]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [AccessLog]s successfully deleted.
	 */
	fun deleteAccessLogs(ids: List<String>): Flow<DocIdentifier>

	/**
	 * Deletes an [AccessLog].
	 *
	 * @param id the id of the [AccessLog] to delete.
	 * @return a [DocIdentifier] related to the [AccessLog] if the operation completes successfully.
	 * @throws AccessDeniedException if the current user does not have the permission to delete the [AccessLog].
	 * @throws NotFoundRequestException if an [AccessLog] with the specified [id] does not exist.
	 */
	suspend fun deleteAccessLog(id: String): DocIdentifier

	/**
	 * Retrieves the all the [AccessLog]s given the [hcPartyId] (and its access keys if it is the current user making
	 * the request) and a set of [AccessLog.secretForeignKeys].
	 * This method will automatically filter out all the [AccessLog] that the current user is not allowed to access.
	 *
	 * @param hcPartyId the id of a data owner.
	 * @param secretForeignKeys a [List] of [AccessLog.secretForeignKeys].
	 * @return a [Flow] of [AccessLog]s.
	 * @throws AccessDeniedException if the current user does not have the permission to get access logs by healthcare
	 * party id.
	 */
	fun listAccessLogsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretForeignKeys: List<String>): Flow<AccessLog>
	suspend fun getAccessLog(accessLogId: String): AccessLog?

	/**
	 * Retrieves all the [AccessLog]s which [AccessLog.created] date is between [fromEpoch] and [toEpoch] in a flow for
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
	 * if present, and [AccessLog.date] equals [startDate], if present, in a [Flow] for pagination.
	 *
	 * @param userId the id of the user the [AccessLog]s refer to.
	 * @param accessType the type of access to the [AccessLog].
	 * @param startDate only the [AccessLog] which [AccessLog.date] is after this date, if present, will be considered.
	 * @param pagination a [PaginationOffset] for pagination.
	 * @param descending whether the results should be ordered in descending or in ascending order by key.
	 * @return a [Flow] of [PaginationElement]s
	 */
	fun findAccessLogsByUserAfterDate(userId: String, accessType: String?, startDate: Long?, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<PaginationElement>
	suspend fun modifyAccessLog(accessLog: AccessLog): AccessLog?
	fun getGenericLogic(): AccessLogLogic
	suspend fun aggregatePatientByAccessLogs(userId: String, accessType: String?, startDate: Long?, startKey: String?, startDocumentId: String?, limit: Int): AggregatedAccessLogs

	/**
	 * Retrieves the ids of all the [AccessLog]s given the [dataOwnerId] (and its access keys if it is the current
	 * user making the request) and a set of [AccessLog.secretForeignKeys].
	 * Only the ids of the Access Logs where [AccessLog.date] is not null are returned and the results are sorted by
	 * [AccessLog.date] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param dataOwnerId the id of the Data Owner allowed to access the [AccessLog]s.
	 * @param secretForeignKeys a [Set] of [AccessLog.secretForeignKeys].
	 * @param startDate if not null, only the ids of the Access Logs where [AccessLog.date] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate if not null, only the ids of the Access Logs where [AccessLog.date] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [AccessLog.date] ascending or descending.
	 * @return a [Flow] of Access Log ids.
	 * @throws AccessDeniedException if the current user does not have the permission to get access logs by healthcare
	 * party id.
	 */
	fun listAccessLogIdsByDataOwnerPatientDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
}
