/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.entities.Agenda
import org.taktik.icure.exceptions.NotFoundRequestException

interface AgendaService {
	suspend fun createAgenda(agenda: Agenda): Agenda?

	/**
	 * Deletes [Agenda]s in batch.
	 * If the user does not meet the precondition to delete [Agenda]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [Set] containing the ids of the [Agenda]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [Agenda]s that were successfully deleted.
	 */
	fun deleteAgendas(ids: Set<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [Agenda].
	 *
	 * @param agendaId the id of the [Agenda] to delete.
	 * @return a [DocIdentifier] related to the [Agenda] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [Agenda].
	 * @throws [NotFoundRequestException] if an [Agenda] with the specified [agendaId] does not exist.
	 */
	suspend fun deleteAgenda(agendaId: String): DocIdentifier
	suspend fun getAgenda(agendaId: String): Agenda?
	suspend fun modifyAgenda(agenda: Agenda): Agenda?
	fun getAgendasByUser(userId: String): Flow<Agenda>
	fun getReadableAgendaForUser(userId: String): Flow<Agenda>

	/**
	 * @return a [Flow] containing all the [Agenda]s the current user can access.
	 */
	fun getAllAgendas(): Flow<Agenda>
}
