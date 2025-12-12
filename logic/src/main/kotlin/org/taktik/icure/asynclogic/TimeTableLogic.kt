/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.entities.TimeTable

interface TimeTableLogic : EntityPersister<TimeTable> {
	suspend fun createTimeTable(timeTable: TimeTable): TimeTable
	suspend fun getTimeTable(timeTableId: String): TimeTable?
	fun getTimeTablesByPeriodAndAgendaId(startDate: Long, endDate: Long, agendaId: String): Flow<TimeTable>
	fun getTimeTablesByAgendaId(agendaId: String): Flow<TimeTable>
}
