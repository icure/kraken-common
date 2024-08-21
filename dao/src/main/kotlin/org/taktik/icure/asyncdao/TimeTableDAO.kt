/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.TimeTable

interface TimeTableDAO : GenericDAO<TimeTable> {

	/**
	 * Retrieves the ids of all the [TimeTable]s where [TimeTable.agendaId] is equal to [agendaId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param agendaId the id of the agenda to use as key.
	 * @return a [Flow] of [TimeTable]s.
	 */
	fun listTimeTablesByAgendaId(datastoreInformation: IDatastoreInformation, agendaId: String): Flow<TimeTable>

	/**
	 * Retrieves the [TimeTable]s where the [TimeTable.startTime] to [TimeTable.endTime] interval
	 * intersects with the [startDate] to [endDate] interval, and where [TimeTable.agendaId] is equal to [agendaId].
	 * If [TimeTable.startTime] is null, then it will be considered as always intersecting the start of the interval.
	 * If [TimeTable.endTime] is null, then it will be considered as always intersecting the end of the interval.
	 * If [startDate] is null, then the interval will start at 0.
	 * If [endDate] is null, then the interval will end at an arbitrary big number.
	 * The results will be sorted by [TimeTable.startTime] in descending or ascending order according to the [descending]
	 * parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param startDate the start of the interval as fuzzy date.
	 * @param endDate the end of the interval as fuzzy date.
	 * @param agendaId the id of the agenda.
	 * @param descending whether to sort the results in descending or ascending order by [TimeTable.startTime].
	 * @return a [Flow] of [TimeTable]s.
	 */
	fun listTimeTablesByPeriodAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String, descending: Boolean): Flow<TimeTable>
	fun listTimeTablesByAgendaIds(datastoreInformation: IDatastoreInformation, agendaIds: Collection<String>): Flow<TimeTable>
	fun listTimeTablesByPeriodAndAgendaIds(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaIds: Collection<String>): Flow<TimeTable>

	/**
	 * Retrieves the ids of all the [TimeTable]s where [TimeTable.agendaId] is equal to [agendaId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param agendaId the id of the agenda to use as key.
	 * @return a [Flow] of [TimeTable.id]s.
	 */
	fun listTimeTableIdsByAgendaId(datastoreInformation: IDatastoreInformation, agendaId: String): Flow<String>

	/**
	 * Retrieves the ids of all the [TimeTable]s where the [TimeTable.startTime] to [TimeTable.endTime] interval
	 * intersects with the [startDate] to [endDate] interval, and where [TimeTable.agendaId] is equal to [agendaId].
	 * If [TimeTable.startTime] is null, then it will be considered as always intersecting the start of the interval.
	 * If [TimeTable.endTime] is null, then it will be considered as always intersecting the end of the interval.
	 * If [startDate] is null, then the interval will start at 0.
	 * If [endDate] is null, then the interval will end at an arbitrary big number.
	 * The results will be sorted by [TimeTable.startTime] in descending or ascending order according to the [descending]
	 * parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param startDate the start of the interval as fuzzy date.
	 * @param endDate the end of the interval as fuzzy date.
	 * @param agendaId the id of the agenda.
	 * @param descending whether to sort the results in descending or ascending order by [TimeTable.startTime].
	 * @return a [Flow] of [TimeTable.id]s.
	 */
	fun listTimeTableIdsByPeriodAndAgendaId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long?,
		endDate: Long?,
		agendaId: String,
		descending: Boolean
	): Flow<String>
}
