/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncdao.AgendaDAO
import org.taktik.icure.asynclogic.AgendaLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Agenda
import org.taktik.icure.exceptions.DeletionException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class AgendaLogicImpl(
    private val agendaDAO: AgendaDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer
) : GenericLogicImpl<Agenda, AgendaDAO>(fixer, datastoreInstanceProvider), AgendaLogic {

	override fun getAllPaginated(offset: PaginationOffset<Nothing>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(agendaDAO
			.getAllPaginated(datastoreInformation, offset.limitIncludingKey(), Nothing::class.java)
			.toPaginatedFlow<Agenda>(offset.limit)
		)
	}

	override suspend fun createAgenda(agenda: Agenda) = fix(agenda) { fixedAgenda ->
		val datastoreInformation = getInstanceAndGroup()
		agendaDAO.create(datastoreInformation, fixedAgenda)
	}

	override fun deleteAgendas(ids: Set<String>): Flow<DocIdentifier> {
		return try {
			deleteEntities(ids)
		} catch (e: Exception) {
			throw DeletionException(e.message, e)
		}
	}

	override suspend fun getAgenda(agenda: String): Agenda? {
		val datastoreInformation = getInstanceAndGroup()
		return agendaDAO.get(datastoreInformation, agenda)
	}

	override suspend fun modifyAgenda(agenda: Agenda) = fix(agenda) { fixedAgenda ->
		val datastoreInformation = getInstanceAndGroup()
		agendaDAO.save(datastoreInformation, fixedAgenda)
	}

	override fun getAgendasByUser(userId: String) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(agendaDAO.getAgendasByUser(datastoreInformation, userId))
	}

	override fun getReadableAgendaForUser(userId: String) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(agendaDAO.getReadableAgendaByUser(datastoreInformation, userId))
	}

	override fun getGenericDAO(): AgendaDAO {
		return agendaDAO
	}
}
