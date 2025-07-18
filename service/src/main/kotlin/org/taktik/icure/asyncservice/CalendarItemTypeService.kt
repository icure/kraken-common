/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException

interface CalendarItemTypeService {
	suspend fun createCalendarItemType(calendarItemType: CalendarItemType): CalendarItemType?

	/**
	 * Deletes [CalendarItemType]s in batch.
	 * If the user does not meet the precondition to delete [CalendarItemType]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [List] containing the ids of the [CalendarItemType]s to delete.
	 * @return a [Flow] containing the deleted [CalendarItemType]s.
	 */
	fun deleteCalendarItemTypes(ids: List<String>): Flow<CalendarItemType>
	suspend fun getCalendarItemType(calendarItemTypeId: String): CalendarItemType?
	fun getCalendarItemTypes(calendarItemTypeIds: Collection<String>): Flow<CalendarItemType>

	/**
	 * Deletes an entity.
	 * An entity deleted this way can't be restored.
	 *
	 * @param id the id of the entity
	 * @param rev the latest known revision of the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun purgeCalendarItemType(id: String, rev: String): DocIdentifier

	/**
	 * Retrieves all the [CalendarItemType]s in a group in a format for pagination.
	 *
	 * @param offset a [PaginationOffset] of [Nothing] (i.e. with an always null key) for pagination.
	 * @return a [Flow] of [PaginationElement]s containing the [CalendarItemType]s.
	 */
	fun getAllCalendarItemTypes(offset: PaginationOffset<Nothing>): Flow<PaginationElement>

	/**
	 * Retrieves all the [CalendarItemType]s in a group.
	 *
	 * @return a [Flow] of [CalendarItemType]s.
	 */
	fun getAllCalendarItemTypes(): Flow<CalendarItemType>
	suspend fun modifyCalendarItemType(calendarItemType: CalendarItemType): CalendarItemType?

	/**
	 * Retrieves all the [CalendarItemType]s for a given [agendaId].
	 *
	 * @return a [Flow] of [CalendarItemType]s.
	 * @throws AccessDeniedException if the current user is not an admin or a healthcare party.
	 */
	fun listCalendarItemTypesByAgendId(agendaId: String): Flow<CalendarItemType>

	/**
	 * Retrieves all the [CalendarItemType]s in a group in a format for pagination, including all the entities where
	 * [CalendarItemType.deletionDate] is not null.
	 *
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [PaginationElement]s containing the [CalendarItemType]s.
	 * @throws AccessDeniedException if the current user is not an admin or a healthcare party.
	 */
	fun getAllEntitiesIncludeDeleted(offset: PaginationOffset<String>): Flow<PaginationElement>

	/**
	 * Retrieves all the [CalendarItemType]s in a group, including all the entities where
	 * [CalendarItemType.deletionDate] is not null.
	 *
	 * @return a [Flow] of [PaginationElement]s containing the [CalendarItemType]s.
	 * @throws AccessDeniedException if the current user is not an admin or a healthcare party.
	 */
	fun getAllEntitiesIncludeDeleted(): Flow<CalendarItemType>
}
