/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.TimeTable
import org.taktik.icure.exceptions.NotFoundRequestException

interface TimeTableService : EntityWithSecureDelegationsService<TimeTable> {
	suspend fun createTimeTable(timeTable: TimeTable): TimeTable?

	/**
	 * Deletes [TimeTable]s in batch.
	 * If the user does not meet the precondition to delete [TimeTable]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [List] containing the ids of the [TimeTable]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [TimeTable]s that were successfully deleted.
	 */
	fun deleteTimeTables(ids: List<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [TimeTable].
	 *
	 * @param timeTableId the id of the [TimeTable] to delete.
	 * @return a [DocIdentifier] related to the [TimeTable] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [TimeTable].
	 * @throws [NotFoundRequestException] if an [TimeTable] with the specified [timeTableId] does not exist.
	 */
	suspend fun deleteTimeTable(timeTableId: String): DocIdentifier
	suspend fun getTimeTable(timeTableId: String): TimeTable?
	fun getTimeTables(ids: List<String>): Flow<TimeTable>
	fun getTimeTablesByPeriodAndAgendaId(startDate: Long, endDate: Long, agendaId: String): Flow<TimeTable>
	fun getTimeTablesByAgendaId(agendaId: String): Flow<TimeTable>
	suspend fun modifyTimeTable(timeTable: TimeTable): TimeTable

	/**
	 * Retrieves the ids of the [TimeTable]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [TimeTable].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchTimeTablesBy(filter: AbstractFilter<TimeTable>): Flow<String>
}
