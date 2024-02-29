/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItem

interface CalendarItemDAO : GenericDAO<CalendarItem> {

	/**
	 * Retrieves all [CalendarItem]s in a group in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param offset a [PaginationOffset] of [Nothing] (i.e. with an always null key) for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [CalendarItem]s.
	 */
	fun listAllCalendarItems(datastoreInformation: IDatastoreInformation, offset: PaginationOffset<Nothing>): Flow<ViewQueryResultEvent>

	fun listCalendarItemByStartDateAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem>

	fun listCalendarItemByStartDateAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String): Flow<CalendarItem>

	fun listCalendarItemByEndDateAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem>

	fun listCalendarItemByEndDateAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agenda: String): Flow<CalendarItem>

	fun listCalendarItemByPeriodAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem>

	fun listCalendarItemByPeriodAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String): Flow<CalendarItem>

	fun listCalendarItemsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<CalendarItem>

	fun findCalendarItemsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, hcPartyId: String, secretPatientKey: String, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	fun findCalendarItemsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, hcPartyId: String, secretPatientKeys: List<String>, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	fun listCalendarItemsByRecurrenceId(datastoreInformation: IDatastoreInformation, recurrenceId: String): Flow<CalendarItem>
}
