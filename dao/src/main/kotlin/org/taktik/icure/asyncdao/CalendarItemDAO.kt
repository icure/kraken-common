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
	fun listCalendarItemByStartDateAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem>

	fun listCalendarItemByStartDateAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String): Flow<CalendarItem>

	fun listCalendarItemByEndDateAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem>

	fun listCalendarItemByEndDateAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agenda: String): Flow<CalendarItem>

	fun listCalendarItemByPeriodAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem>

	fun listCalendarItemByPeriodAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String): Flow<CalendarItem>

	fun listCalendarItemsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<CalendarItem>

	fun findCalendarItemsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, hcPartyId: String, secretPatientKey: String, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	fun findCalendarItemsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, hcPartyId: String, secretPatientKeys: List<String>, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [CalendarItem]s in a group where [CalendarItem.recurrenceId] is equal to the provided [recurrenceId]
	 * in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param recurrenceId the [CalendarItem.recurrenceId].
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [CalendarItem]s.
	 */
	fun listCalendarItemsByRecurrenceId(datastoreInformation: IDatastoreInformation, recurrenceId: String, offset: PaginationOffset<String>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [CalendarItem]s in a group where [CalendarItem.recurrenceId] is equal to the provided [recurrenceId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param recurrenceId the [CalendarItem.recurrenceId].
	 * @return a [Flow] of [CalendarItem]s.
	 */
	fun listCalendarItemsByRecurrenceId(datastoreInformation: IDatastoreInformation, recurrenceId: String): Flow<CalendarItem>

	/**
	 * Retrieves the ids of all the [CalendarItem]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of
	 * [CalendarItem.secretForeignKeys].
	 * Only the ids of the Calendar Items where [CalendarItem.startTime] is not null are returned and the results are sorted by
	 * [CalendarItem.startTime] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [CalendarItem.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the Calendar Items where [CalendarItem.startTime] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the Calendar Items where [CalendarItem.startTime] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [CalendarItem.startTime] ascending or descending.
	 * @return a [Flow] of Calendar Item ids.
	 */
	fun listCalendarItemsIdsByDataOwnerPatientStartTime(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
}
