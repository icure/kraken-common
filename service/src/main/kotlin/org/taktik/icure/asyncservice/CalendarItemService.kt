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
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.exceptions.NotFoundRequestException

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
	fun listCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<CalendarItem>

	suspend fun modifyCalendarItem(calendarItem: CalendarItem): CalendarItem?
	fun getAllCalendarItems(): Flow<CalendarItem>
	fun getCalendarItems(ids: List<String>): Flow<CalendarItem>
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
}
