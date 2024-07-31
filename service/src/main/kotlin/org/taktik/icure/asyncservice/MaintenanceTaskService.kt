/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.exceptions.NotFoundRequestException

interface MaintenanceTaskService : EntityWithSecureDelegationsService<MaintenanceTask> {

	/**
	 * Deletes [MaintenanceTask]s in batch.
	 * If the user does not meet the precondition to delete [MaintenanceTask]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [Collection] containing the ids of the [MaintenanceTask]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [MaintenanceTask]s that were successfully deleted.
	 */
	fun deleteMaintenanceTasks(ids: Collection<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [MaintenanceTask].
	 *
	 * @param maintenanceTaskId the id of the [MaintenanceTask] to delete.
	 * @return a [DocIdentifier] related to the [MaintenanceTask] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [MaintenanceTask].
	 * @throws [NotFoundRequestException] if an [MaintenanceTask] with the specified [maintenanceTaskId] does not exist.
	 */
	suspend fun deleteMaintenanceTask(maintenanceTaskId: String): DocIdentifier
	suspend fun modifyMaintenanceTask(entity: MaintenanceTask): MaintenanceTask?
	suspend fun createMaintenanceTask(entity: MaintenanceTask): MaintenanceTask?
	fun modifyMaintenanceTasks(entities: Collection<MaintenanceTask>): Flow<MaintenanceTask>

	/**
	 * Retrieves all the [MaintenanceTask]s from the database that match the provided [FilterChain], using the provided
	 * [PaginationOffset] to skip all the result up to a certain result and to set the number of returned results.
	 * Each entity in the returning flow will be wrapped in a [ViewQueryResultEvent].
	 *
	 * @param paginationOffset a [PaginationOffset] that specifies where to start filtering and the result number limit.
	 * @param filter a [FilterChain] to filter the [MaintenanceTask]s.
	 * @return a [Flow] of [ViewQueryResultEvent], each one wrapping a matching [MaintenanceTask]. This [Flow] will
	 * contain only the maintenance tasks that the current user can access.
	 * @throws [AccessDeniedException] if the user does not have the permission to search the [MaintenanceTask]s using the specified filter.
	 */
	fun filterMaintenanceTasks(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<MaintenanceTask>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves a single [MaintenanceTask] by its id.
	 *
	 * @param id the id of the [MaintenanceTask] to retrieve.
	 * @return the [MaintenanceTask].
	 * @throws [AccessDeniedException] if the user does not have the permission to access that [MaintenanceTask].
	 */
	suspend fun getMaintenanceTask(id: String): MaintenanceTask?

	/**
	 * Retrieves a multiple [MaintenanceTask]s by their ids. It will automatically filter out the entities that the
	 * current user cannot access.
	 *
	 * @param ids a [List] containing the ids of the entities to retrieve.
	 * @return a [Flow] containing the matching [MaintenanceTask]s.
	 * @throws [AccessDeniedException] if the user does not have the permission to search maintenance tasks.
	 */
	fun getMaintenanceTasks(ids: List<String>): Flow<MaintenanceTask>

	/**
	 * Creates a batch of [MaintenanceTask]s.
	 * @param entities a [Collection] of [MaintenanceTask]s to create.
	 * @return a [Flow] containing all the [MaintenanceTask]s that were successfully creates.
	 * @throws [AccessDeniedException] if the user does not have the permission to create a [MaintenanceTask].
	 */
	fun createMaintenanceTasks(entities: Collection<MaintenanceTask>): Flow<MaintenanceTask>

	/**
	 * Retrieves the ids of the [MaintenanceTask]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [MaintenanceTask].
	 * @param deduplicate whether to remove the duplicate ids from the result, if any.
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchMaintenanceTasksBy(filter: AbstractFilter<MaintenanceTask>, deduplicate: Boolean): Flow<String>
}
