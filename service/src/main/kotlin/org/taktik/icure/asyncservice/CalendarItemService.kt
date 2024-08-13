/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface CalendarItemService : EntityWithSecureDelegationsService<CalendarItem> {
	suspend fun createCalendarItem(calendarItem: CalendarItem): CalendarItem?

	/**
	 * Deletes [CalendarItem]s in batch.
	 * If the user does not meet the precondition to delete [CalendarItem]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [List] containing the ids of the [CalendarItem]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [CalendarItem]s that were successfully deleted.
	 */
	fun deleteCalendarItems(ids: List<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [CalendarItem].
	 *
	 * @param calendarItemId the id of the [CalendarItem] to delete.
	 * @return a [DocIdentifier] related to the [CalendarItem] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [CalendarItem].
	 * @throws [NotFoundRequestException] if an [CalendarItem] with the specified [calendarItemId] does not exist.
	 */
	suspend fun deleteCalendarItem(calendarItemId: String): DocIdentifier
	suspend fun getCalendarItem(calendarItemId: String): CalendarItem?
	fun getCalendarItemByPeriodAndHcPartyId(startDate: Long, endDate: Long, hcPartyId: String): Flow<CalendarItem>
	fun getCalendarItemByPeriodAndAgendaId(startDate: Long, endDate: Long, agendaId: String): Flow<CalendarItem>

	/**
	 * Retrieves the ids of all the [CalendarItem]s given the [dataOwnerId] (plus all the current access keys if that is
	 * equal to the data owner id of the user making the request) and a set of [CalendarItem.secretForeignKeys].
	 * Only the ids of the Calendar Items where [CalendarItem.startTime] is not null are returned and the results are sorted by
	 * [CalendarItem.startTime] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param dataOwnerId the id of a data owner.
	 * @param secretForeignKeys a [Set] of [CalendarItem.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the Calendar Items where [CalendarItem.startTime] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the Calendar Items where [CalendarItem.startTime] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [CalendarItem.startTime] ascending or descending.
	 * @return a [Flow] of Calendar Item ids.
	 * @throws AccessDeniedException if [dataOwnerId] is not the current data owner id and is not among the access keys
	 * and the current user does not have the permission to search Calendar Items for other users.
	 */
	fun findCalendarItemIdsByDataOwnerPatientStartTime(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
	fun listCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<CalendarItem>

	suspend fun modifyCalendarItem(calendarItem: CalendarItem): CalendarItem?

	/**
	 * Retrieves all [CalendarItem]s in a group in a format for pagination.
	 * Note: this method will automatically filter out the entities that the current user is not allowed to access, but
	 * it guarantees that the page size specified in the [offset] is reached as long as there are available entities.
	 *
	 * @param offset a [PaginationOffset] of [Nothing] (i.e. with an always null key) for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [CalendarItem]s.
	 */
	fun getAllCalendarItems(offset: PaginationOffset<Nothing>): Flow<PaginationElement>

	/**
	 * Retrieves all [CalendarItem]s in a group.
	 * Note: this method will automatically filter out the entities that the current user is not allowed to access.
	 *
	 * @return a [Flow] of [CalendarItem]s.
	 */
	fun getAllCalendarItems(): Flow<CalendarItem>
	fun getCalendarItems(ids: List<String>): Flow<CalendarItem>

	/**
	 * Retrieves all the [CalendarItem]s in a group where [CalendarItem.recurrenceId] is equal to the provided [recurrenceId]
	 * in a format for pagination.
	 * Note: this method will automatically filter out the entities that the current user is not allowed to access, but
	 * it guarantees that the page size specified in the [paginationOffset] is reached as long as there are available entities.
	 *
	 * @param recurrenceId the [CalendarItem.recurrenceId].
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [CalendarItem]s.
	 */
	fun getCalendarItemsByRecurrenceId(recurrenceId: String, paginationOffset: PaginationOffset<String>): Flow<PaginationElement>

	/**
	 * Retrieves all the [CalendarItem]s in a group where [CalendarItem.recurrenceId] is equal to the provided [recurrenceId].
	 * Note: this method will automatically filter out the entities that the current user is not allowed to access.
	 *
	 * @param recurrenceId the [CalendarItem.recurrenceId].
	 * @return a [Flow] of [CalendarItem]s.
	 */
	fun getCalendarItemsByRecurrenceId(recurrenceId: String): Flow<CalendarItem>

	/**
	 * Updates a collection of [CalendarItem]s.
	 *
	 * This method will automatically filter out all the changes that the user is not authorized to make, either because
	 * they are not valid or because the user does not have the correct permission to apply them. No error will be
	 * returned for the filtered out entities.
	 *
	 * @param entities a [Collection] of updated [CalendarItem].
	 * @return a [Flow] containing all the [CalendarItem]s successfully updated.
	 */
	fun modifyEntities(entities: Collection<CalendarItem>): Flow<CalendarItem>
	fun findCalendarItemsByHCPartyAndSecretPatientKeys(
		hcPartyId: String,
		secretPatientKeys: List<String>,
		paginationOffset: PaginationOffset<List<Any>>
	): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves the ids of the [CalendarItem]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [CalendarItem].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchCalendarItemsBy(filter: AbstractFilter<CalendarItem>): Flow<String>
}
