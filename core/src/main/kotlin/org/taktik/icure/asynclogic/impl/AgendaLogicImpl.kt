/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.taktik.icure.asyncdao.AgendaDAO
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.AgendaLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.config.SdkVersionConfig
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Agenda
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

open class AgendaLogicImpl(
	private val agendaDAO: AgendaDAO,
	private val sdkVersionConfig: SdkVersionConfig,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : GenericLogicImpl<Agenda, AgendaDAO>(fixer, datastoreInstanceProvider, filters),
	AgendaLogic {
	override fun getAllPaginated(offset: PaginationOffset<Nothing>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			agendaDAO
				.getAllPaginated(datastoreInformation, offset.limitIncludingKey(), Nothing::class.java)
				.toPaginatedFlow<Agenda>(offset.limit),
		)
	}

	override suspend fun createAgenda(agenda: Agenda) = fix(agenda, isCreate = true) { fixedAgenda ->
		if (sdkVersionConfig.hasAtLeastFeatureLevelOf(SdkVersionConfig.FeatureLevel.AccessLogUserRights)) {
			require(fixedAgenda.userRights.isNotEmpty()) {
				"You cannot create an Agenda with empty userRights"
			}
		}
		val datastoreInformation = getInstanceAndGroup()
		agendaDAO.create(datastoreInformation, fixedAgenda)
	}

	override fun createAgendas(agendas: List<Agenda>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			agendaDAO.createBulk(
				datastoreInformation,
				agendas.map {
					fix(it, isCreate = true).also { fixedAgenda ->
						if (sdkVersionConfig.hasAtLeastFeatureLevelOf(SdkVersionConfig.FeatureLevel.AccessLogUserRights)) {
							require(fixedAgenda.userRights.isNotEmpty()) {
								"You cannot create an Agenda with empty userRights"
							}
						}
					}
				}
			).filterSuccessfulUpdates()
		)
	}

	override suspend fun getAgenda(agenda: String): Agenda? {
		val datastoreInformation = getInstanceAndGroup()
		return agendaDAO.get(datastoreInformation, agenda)
	}

	override suspend fun modifyAgenda(agenda: Agenda) = fix(agenda, isCreate = false) { fixedAgenda ->
		val datastoreInformation = getInstanceAndGroup()
		agendaDAO.save(datastoreInformation, fixedAgenda)
	}

	override fun getAgendasByUser(userId: String) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(agendaDAO.getAgendasByUser(datastoreInformation, userId))
	}

	override fun getReadableAgendaForUserLegacy(userId: String) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(agendaDAO.getReadableAgendaByUserLegacy(datastoreInformation, userId))
	}

	override fun getGenericDAO(): AgendaDAO = agendaDAO
}
