/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException

interface MaintenanceTaskService : EntityWithSecureDelegationsService<MaintenanceTask> {
	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [MaintenanceTask]s.
	 */
	fun deleteMaintenanceTasks(ids: List<IdAndRev>): Flow<MaintenanceTask>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the latest rev of the entity to delete.
	 * @return the deleted [MaintenanceTask].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteMaintenanceTask(id: String, rev: String?): MaintenanceTask

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
	suspend fun purgeMaintenanceTask(id: String, rev: String): DocIdentifier

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity
	 * @param id the id of the entity marked to restore
	 * @param rev the revision of the entity after it was marked as deleted
	 * @return the restored entity
	 */
	suspend fun undeleteMaintenanceTask(id: String, rev: String): MaintenanceTask
	suspend fun modifyMaintenanceTask(entity: MaintenanceTask): MaintenanceTask?
	suspend fun createMaintenanceTask(entity: MaintenanceTask): MaintenanceTask
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
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchMaintenanceTasksBy(filter: AbstractFilter<MaintenanceTask>): Flow<String>
}
