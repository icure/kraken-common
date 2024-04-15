/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.pagination.PaginationElement

interface CalendarItemLogic : EntityPersister<CalendarItem, String>, EntityWithSecureDelegationsLogic<CalendarItem> {
	suspend fun createCalendarItem(calendarItem: CalendarItem): CalendarItem?
	fun deleteCalendarItems(ids: List<String>): Flow<DocIdentifier>
	fun deleteCalendarItems(ids: Flow<String>): Flow<DocIdentifier>
	suspend fun getCalendarItem(calendarItemId: String): CalendarItem?
	fun getCalendarItemByPeriodAndHcPartyId(startDate: Long, endDate: Long, hcPartyId: String): Flow<CalendarItem>
	fun getCalendarItemByPeriodAndAgendaId(startDate: Long, endDate: Long, agendaId: String): Flow<CalendarItem>
	fun listCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<CalendarItem>
	fun findCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>, paginationOffset: PaginationOffset<List<Any>>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all [CalendarItem]s in a group in a format for pagination.
	 *
	 * @param offset a [PaginationOffset] of [Nothing] (i.e. with an always null key) for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [CalendarItem]s.
	 */
	fun getAllCalendarItems(offset: PaginationOffset<Nothing>): Flow<PaginationElement>
	fun getCalendarItems(ids: List<String>): Flow<CalendarItem>

	/**
	 * Retrieves all the [CalendarItem]s in a group where [CalendarItem.recurrenceId] is equal to the provided [recurrenceId]
	 * in a format for pagination.
	 *
	 * @param recurrenceId the [CalendarItem.recurrenceId].
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [CalendarItem]s.
	 */
	fun getCalendarItemsByRecurrenceId(recurrenceId: String, paginationOffset: PaginationOffset<String>): Flow<PaginationElement>

	/**
	 * Retrieves all the [CalendarItem]s in a group where [CalendarItem.recurrenceId] is equal to the provided [recurrenceId].
	 *
	 * @param recurrenceId the [CalendarItem.recurrenceId].
	 * @return a [Flow] of [CalendarItem]s.
	 */
	fun getCalendarItemsByRecurrenceId(recurrenceId: String): Flow<CalendarItem>

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
	 */
	fun listCalendarItemIdsByDataOwnerPatientStartTime(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
}
