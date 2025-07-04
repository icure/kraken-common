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
	 * List calendar items with start date between the [searchStart] and [searchEnd] (exclusive), and for the provided
	 * agenda id.
	 * If [lastKnownDocumentId] is not null return all entries after that
	 * TODO since needed only for availabilities return type will be replaced by calendar item stub
	 */
	fun listCalendarItemByStartDateAndAgendaId(
		datastoreInformation: IDatastoreInformation,
		searchStart: Long,
		searchEnd: Long,
		agendaId: String,
		limit: Int,
		lastKnownDocumentId: String?
	): Flow<CalendarItem>

	fun listCalendarItemByStartDateAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem>

	fun listCalendarItemByEndDateAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem>

	fun listCalendarItemByPeriodAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem>

	/**
	 * Retrieves all the [CalendarItem.id]s with a delegation for the specified [searchKey], where the max among [CalendarItem.created],
	 * [CalendarItem.modified], and [CalendarItem.deletionDate] is greater or equal than [startTimestamp] (if provided) and less or equal
	 * than [endTimestamp] (if provided).
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKey a data owner id or a search key.
	 * @param startTimestamp the lower bound for the latest update date.
	 * @param endTimestamp the upper bound for the latest update date.
	 * @param descending whether to sort the results by last update in ascending or descending order.
	 * @return a [Flow] of [CalendarItem.id]s.
	 */
	fun listCalendarItemIdsByDataOwnerLifecycleBetween(datastoreInformation: IDatastoreInformation, searchKey: String, startTimestamp: Long?, endTimestamp: Long?, descending: Boolean): Flow<String>

	/**
	 * Retrieves all the [CalendarItem]s in a group where [CalendarItem.agendaId] is equal to the provided [agendaId],
	 * [CalendarItem.startTime] is greater or equal than [startDate], and [CalendarItem.endTime] is less
	 * or equal than [endDate].
	 * The entities will be sorted by [CalendarItem.startTime] in ascending or descending order according to the value
	 * of [descending].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param startDate a fuzzy date.
	 * @param endDate a fuzzy date. If null, all the [CalendarItem]s until the end of time will be returned.
	 * @param agendaId the [CalendarItem.agendaId].
	 * @return a [Flow] containing the matching [CalendarItem]s.
	 */
	fun listCalendarItemByPeriodAndAgendaId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long,
		endDate: Long,
		agendaId: String,
		descending: Boolean
	): Flow<CalendarItem>

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
	 * Retrieves all the [CalendarItem.id]s in a group where [CalendarItem.recurrenceId] is equal to the provided [recurrenceId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param recurrenceId the [CalendarItem.recurrenceId].
	 * @return a [Flow] of [CalendarItem.id]s.
	 */
	fun listCalendarItemIdsByRecurrenceId(
		datastoreInformation: IDatastoreInformation,
		recurrenceId: String
	): Flow<String>

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
	 * @return a [Flow] of [CalendarItem.id]s.
	 */
	fun listCalendarItemIdsByDataOwnerPatientStartTime(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	/**
	 * Retrieves the ids of all the [CalendarItem]s given a [dataOwnerId] (that can be a data owner id or a search key).
	 * where either [CalendarItem.startTime] or [CalendarItem.endTime] are between [startDate] and [endDate].
	 * If [startDate] is null, then no lower bound for the interval is set.
	 * If [endDate] is null, then no upper bound for the interval is set.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param dataOwnerId the id of the data owner, or a search key for the data owner.
	 * @param startDate a fuzzy date. If not null, it defines the lower bound of the desired interval.
	 * @param endDate a fuzzy date. If not null, it defines the upper bound of the desired interval.
	 * @return a [Flow] of [CalendarItem.id]s.
	 */
	fun listCalendarItemIdsByPeriodAndDataOwnerId(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		startDate: Long?,
		endDate: Long?
	): Flow<String>
}
