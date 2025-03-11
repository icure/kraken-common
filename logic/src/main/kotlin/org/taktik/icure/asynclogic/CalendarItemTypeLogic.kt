/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.pagination.PaginationElement

interface CalendarItemTypeLogic : EntityPersister<CalendarItemType> {
	suspend fun createCalendarItemType(calendarItemType: CalendarItemType): CalendarItemType?

	suspend fun getCalendarItemType(calendarItemTypeId: String): CalendarItemType?
	fun getCalendarItemTypes(calendarItemTypeIds: Collection<String>): Flow<CalendarItemType>

	/**
	 * Retrieves all the [CalendarItemType]s in a group in a format for pagination.
	 *
	 * @param offset a [PaginationOffset] of [Nothing] (i.e. with an always null key) for pagination.
	 * @return a [Flow] of [PaginationElement]s containing the [CalendarItemType]s.
	 */
	fun getAllCalendarItemTypes(offset: PaginationOffset<Nothing>): Flow<PaginationElement>
	suspend fun modifyCalendarTypeItem(calendarItemType: CalendarItemType): CalendarItemType?

	/**
	 * Retrieves all the [CalendarItemType]s for a given [agendaId].
	 *
	 * @return a [Flow] of [CalendarItemType]s.
	 */
	fun listCalendarItemTypesByAgendId(agendaId: String): Flow<CalendarItemType>

	/**
	 * Retrieves all the [CalendarItemType]s in a group in a format for pagination, including all the entities where
	 * [CalendarItemType.deletionDate] is not null.
	 *
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [PaginationElement]s containing the [CalendarItemType]s.
	 */
	fun getAllEntitiesIncludeDeleted(offset: PaginationOffset<String>): Flow<PaginationElement>

	/**
	 * Retrieves all the [CalendarItemType]s in a group, including all the entities where
	 * [CalendarItemType.deletionDate] is not null.
	 *
	 * @return a [Flow] of [CalendarItemType]s.
	 */
	fun getAllEntitiesIncludeDeleted(): Flow<CalendarItemType>
}
