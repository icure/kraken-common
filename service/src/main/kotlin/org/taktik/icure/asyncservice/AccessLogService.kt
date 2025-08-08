/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.AccessLogLogic
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.result.AggregatedAccessLogs
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface AccessLogService : EntityWithSecureDelegationsService<AccessLog> {
	suspend fun createAccessLog(accessLog: AccessLog): AccessLog?

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [AccessLog]s.
	 */
	fun deleteAccessLogs(ids: List<IdAndRev>): Flow<AccessLog>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the rev of the entity to delete.
	 * @return the deleted [AccessLog].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteAccessLog(id: String, rev: String?): AccessLog

	/**
	 * Deletes an entity.
	 * An entity deleted this way can't be restored.
	 * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
	 *
	 * @param id the id of the entity
	 * @param rev the latest known revision of the entity.
	 * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun purgeAccessLog(id: String, rev: String): DocIdentifier

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity
	 * @param id the id of the entity marked to restore
	 * @param rev the revision of the entity after it was marked as deleted
	 * @return the restored entity
	 */
	suspend fun undeleteAccessLog(id: String, rev: String): AccessLog

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
	@Deprecated("This method is inefficient for high volumes of keys, use listAccessLogIdsByDataOwnerPatientDate instead")
	fun listAccessLogsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretForeignKeys: List<String>): Flow<AccessLog>
	suspend fun getAccessLog(accessLogId: String): AccessLog?

	/**
	 * Retrieves all the [AccessLog]s with the provided [ids], filtering out the ones that the current user cannot access.
	 *
	 * @param ids the ids of the [AccessLog] to retrieve.
	 * @return a [Flow] of [AccessLog] that the current user can access.
	 * @throws AccessDeniedException if the current user does not have the permission to get the access logs.
	 */
	fun getAccessLogs(ids: List<String>): Flow<AccessLog>

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
	 * @throws AccessDeniedException if the current user does not have the permission to get [AccessLog]s by healthcare
	 * party id.
	 */
	fun listAccessLogIdsByDataOwnerPatientDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	/**
	 * Retrieves the ids of the [AccessLog]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [AccessLog].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchAccessLogsBy(filter: AbstractFilter<AccessLog>): Flow<String>
}
