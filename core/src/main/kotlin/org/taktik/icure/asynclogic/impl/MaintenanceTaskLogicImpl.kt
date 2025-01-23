/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.TotalCount
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.MaintenanceTaskDAO
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.MaintenanceTaskLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.validation.aspect.Fixer
import java.util.*

@Service
@Profile("app")
class MaintenanceTaskLogicImpl(
    private val maintenanceTaskDAO: MaintenanceTaskDAO,
    filters: Filters,
    sessionLogic: SessionInformationProvider,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    fixer: Fixer
) : EntityWithEncryptionMetadataLogic<MaintenanceTask, MaintenanceTaskDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters), MaintenanceTaskLogic {

	override fun filter(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<MaintenanceTask>): Flow<ViewQueryResultEvent> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			val ids = filters.resolve(filter.filter, datastoreInformation).toSet(TreeSet())
			aggregateResults(
				ids = ids,
				limit = paginationOffset.limit,
				supplier = { mtIds: Collection<String> ->
					maintenanceTaskDAO.findMaintenanceTasksByIds(datastoreInformation, mtIds.asFlow())
			   },
				startDocumentId = paginationOffset.startDocumentId
			).forEach { emit(it) }
			emit(TotalCount(ids.size))
		}

	override fun entityWithUpdatedSecurityMetadata(
		entity: MaintenanceTask,
		updatedMetadata: SecurityMetadata
	): MaintenanceTask {
		return entity.copy(securityMetadata = updatedMetadata)
	}

	override fun getGenericDAO(): MaintenanceTaskDAO {
		return maintenanceTaskDAO
	}

	override suspend fun createMaintenanceTask(maintenanceTask: MaintenanceTask): MaintenanceTask? = fix(maintenanceTask, isCreate = true) {
		if(it.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		createEntities(listOf(it)).firstOrNull()
	}

	override fun createEntities(entities: Collection<MaintenanceTask>) = flow {
		emitAll(
			super.createEntities( entities.map { fix(it, isCreate = true) } )
		)
	}
}

