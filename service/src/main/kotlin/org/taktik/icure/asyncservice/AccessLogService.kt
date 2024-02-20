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
import org.taktik.icure.pagination.PaginatedElement

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
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [AccessLog].
	 * @throws [NotFoundRequestException] if an [AccessLog] with the specified [id] does not exist.
	 */
	suspend fun deleteAccessLog(id: String): DocIdentifier
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
	suspend fun modifyAccessLog(accessLog: AccessLog): AccessLog?
	fun getGenericLogic(): AccessLogLogic
	suspend fun aggregatePatientByAccessLogs(userId: String, accessType: String?, startDate: Long?, startKey: String?, startDocumentId: String?, limit: Int): AggregatedAccessLogs

	/**
	 * Retrieves all the [AccessLog]s by search key and secret patient key with support for pagination.
	 * All the [AccessLog]s that the current user cannot access will be filtered out by the final result.
	 *
	 * @param searchKey the search key.
	 * @param secretPatientKey the secret patient key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginatedElement] containing the [AccessLog]s.
	 * @throws AccessDeniedException if the user does not meet the precondition to execute this
	 */
	fun listAccessLogsBySearchKeyAndSecretPatientKey(
		searchKey: String,
		secretPatientKey: String,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<PaginatedElement>
}
