/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Agenda
import org.taktik.icure.pagination.PaginationElement

interface AgendaLogic : EntityPersister<Agenda> {

	/**
	 * Retrieves all the [Agenda]s in a group in a format for pagination.
	 *
	 * @param offset a [PaginationOffset] of [Nothing] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Agenda]s.
	 */
	fun getAllPaginated(offset: PaginationOffset<Nothing>): Flow<PaginationElement>
	suspend fun createAgenda(agenda: Agenda): Agenda?

	suspend fun getAgenda(agenda: String): Agenda?
	suspend fun modifyAgenda(agenda: Agenda): Agenda?
	fun getAgendasByUser(userId: String): Flow<Agenda>
	fun getReadableAgendaForUserLegacy(userId: String): Flow<Agenda>
}
