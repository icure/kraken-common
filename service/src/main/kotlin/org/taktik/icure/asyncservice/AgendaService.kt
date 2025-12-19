/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Agenda
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface AgendaService {
	suspend fun createAgenda(agenda: Agenda): Agenda
	fun createAgendas(agendas: List<Agenda>): Flow<Agenda>

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [Agenda]s.
	 */
	fun deleteAgendas(ids: List<IdAndRev>): Flow<Agenda>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the rev of the agenda to delete.
	 * @return the deleted [Agenda].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteAgenda(id: String, rev: String?): Agenda

	/**
	 * Deletes an entity.
	 * An entity deleted this way can't be restored.
	 * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
	 *
	 * @param id the id of the entity
	 * @param rev the latest known revision of the entity.
	 * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun purgeAgenda(id: String, rev: String): DocIdentifier

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity
	 * @param id the id of the entity marked to restore
	 * @param rev the revision of the entity after it was marked as deleted
	 * @return the restored entity
	 */
	suspend fun undeleteAgenda(id: String, rev: String): Agenda
	suspend fun getAgenda(agendaId: String): Agenda?
	fun getAgendas(agendaIds: List<String>): Flow<Agenda>

	suspend fun modifyAgenda(agenda: Agenda): Agenda
	fun modifyAgendas(agendas: List<Agenda>): Flow<Agenda>

	/**
	 * Gets agenda where [Agenda.userId] matches the provided [userId]
	 */
	fun getAgendasByUser(userId: String): Flow<Agenda>

	@Deprecated("Based on legacy Agenda.rights ; use filter for agendas using userRights")
	fun getReadableAgendaForUser(userId: String): Flow<Agenda>

	/**
	 * Retrieves all the [Agenda]s in a group in a format for pagination.
	 *
	 * @param offset a [PaginationOffset] of [Nothing] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Agenda]s.
	 */
	fun getAllAgendas(offset: PaginationOffset<Nothing>): Flow<PaginationElement>

	/**
	 * Retrieves all the [Agenda]s in a group.
	 *
	 * @return a [Flow] of [Agenda]s.
	 */
	fun getAllAgendas(): Flow<Agenda>

	/**
	 * Retrieves the ids of the [Agenda]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [Agenda].
	 * @return a [Flow] of the ids matching the filter.
	 */
	fun matchAgendasBy(filter: AbstractFilter<Agenda>): Flow<String>
}
