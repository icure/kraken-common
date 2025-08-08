/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.TimeTableDAO
import org.taktik.icure.asynclogic.TimeTableLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.TimeTable
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class TimeTableLogicImpl(
	private val timeTableDAO: TimeTableDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : GenericLogicImpl<TimeTable, TimeTableDAO>(fixer, datastoreInstanceProvider, filters),
	TimeTableLogic {
	override suspend fun createTimeTable(timeTable: TimeTable) = fix(timeTable, isCreate = true) { fixedTimeTable ->
		if (fixedTimeTable.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		val datastoreInformation = getInstanceAndGroup()
		timeTableDAO.create(datastoreInformation, fixedTimeTable)
	}

	override suspend fun getTimeTable(timeTableId: String): TimeTable? = getEntity(timeTableId)

	override fun getTimeTablesByPeriodAndAgendaId(
		startDate: Long,
		endDate: Long,
		agendaId: String,
	): Flow<TimeTable> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(timeTableDAO.listTimeTablesByPeriodAndAgendaId(datastoreInformation, startDate, endDate, agendaId, false))
	}

	override fun getTimeTablesByAgendaId(agendaId: String): Flow<TimeTable> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(timeTableDAO.listTimeTablesByAgendaId(datastoreInformation, agendaId))
	}

	override fun getGenericDAO(): TimeTableDAO = timeTableDAO
}
