/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.*
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.TotalCount
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.MaintenanceTaskDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.MaintenanceTaskLogic
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import org.taktik.icure.validation.aspect.Fixer
import java.util.*

@Service
@Profile("app")
class MaintenanceTaskLogicImpl(
    private val maintenanceTaskDAO: MaintenanceTaskDAO,
    private val filters: Filters,
    sessionLogic: SessionInformationProvider,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    fixer: Fixer
) : EntityWithEncryptionMetadataLogic<MaintenanceTask, MaintenanceTaskDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic), MaintenanceTaskLogic {

	override fun listMaintenanceTasksByHcPartyAndIdentifier(healthcarePartyId: String, identifiers: List<Identifier>): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(maintenanceTaskDAO.listMaintenanceTasksByHcPartyAndIdentifier(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(healthcarePartyId), identifiers))
	}

	override fun listMaintenanceTasksByHcPartyAndType(healthcarePartyId: String, type: String, startDate: Long?, endDate: Long?): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			mergeUniqueIdsForSearchKeys(getAllSearchKeysIfCurrentDataOwner(healthcarePartyId)) { key ->
				maintenanceTaskDAO.listMaintenanceTasksByHcPartyAndType(
					datastoreInformation,
					key,
					type,
					startDate,
					endDate
				)
			}
		)
	}

	override fun listMaintenanceTasksAfterDate(healthcarePartyId: String, date: Long): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			mergeUniqueIdsForSearchKeys(getAllSearchKeysIfCurrentDataOwner(healthcarePartyId)) { key ->
				maintenanceTaskDAO.listMaintenanceTasksAfterDate(datastoreInformation, key, date)
			}
		)
	}

	override fun filterMaintenanceTasksIds(filter: FilterChain<MaintenanceTask>, limit: Int, startDocumentId: String?): Flow<String> =
		flow {
			val ids = filters.resolve(filter.filter)

			val sortedIds = if (startDocumentId != null) { // Sub-set starting from startDocId to the end (including last element)
				ids.dropWhile { it != startDocumentId }
			} else {
				ids
			}

			emitAll(sortedIds)
		}

	override fun filter(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<MaintenanceTask>): Flow<ViewQueryResultEvent> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			val ids = filters.resolve(filter.filter).toSet(TreeSet())
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

	override suspend fun createMaintenanceTask(maintenanceTask: MaintenanceTask): MaintenanceTask? = fix(maintenanceTask) {
		if(it.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		createEntities(listOf(it)).firstOrNull()
	}

	override fun createEntities(entities: Collection<MaintenanceTask>) = flow {
		emitAll(
			super.createEntities( entities.map { fix(it) } )
		)
	}

	override fun deleteMaintenanceTasks(maintenanceTaskToDeletes: Collection<MaintenanceTask>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(maintenanceTaskDAO.remove(datastoreInformation, maintenanceTaskToDeletes))

	}
}

