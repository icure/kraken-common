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
	fun getCalendarItemsByRecurrenceId(recurrenceId: String): Flow<CalendarItem>
}
