/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.MaintenanceTask

interface MaintenanceTaskLogic : EntityPersister<MaintenanceTask>, EntityWithSecureDelegationsLogic<MaintenanceTask> {
	suspend fun createMaintenanceTask(maintenanceTask: MaintenanceTask): MaintenanceTask?

	/**
	 * Retrieves all the [MaintenanceTask]s from the database that match the provided [FilterChain], using the provided
	 * [PaginationOffset] to skip all the result up to a certain result and to set the number of returned results.
	 * Each entity in the returning flow will be wrapped in a [ViewQueryResultEvent].
	 * @param paginationOffset a [PaginationOffset] that specifies where to start filtering and the result number limit.
	 * @param filter a [FilterChain] to filter the [MaintenanceTask]s.
	 * @return a [Flow] of [ViewQueryResultEvent], each one wrapping a matching [MaintenanceTask].
	 */
	fun filter(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<MaintenanceTask>): Flow<ViewQueryResultEvent>
}
