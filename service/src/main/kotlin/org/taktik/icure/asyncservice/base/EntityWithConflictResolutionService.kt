package org.taktik.icure.asyncservice.base

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.conflicts.ConflictResolutionResult
import org.taktik.icure.entities.conflicts.MergeResult

/**
 * Defines methods which should be common to the logic of all [StoredDocument] entities for which conflicts resolution should be triggered through the API.
 */
interface EntityWithConflictResolutionService<E : StoredDocument> {

	/**
	 * @return a [Flow] containing the ids of all the entities with conflicting revisions.
	 */
	fun getConflictingEntitiesIds(): Flow<String>

	/**
	 * @param entityId the id of an entity with conflicting revisions.
	 * @return a [Flow] containing all the revisions of the entity that are conflicting.
	 * @throws DocumentNotFoundException if the entity does not exist.
	 * @throws IllegalArgumentException if the entity does not have conflicts.
	 */
	fun getConflictsFor(entityId: String): Flow<E>

	/**
	 * Sets one revision of the entity as winner of the conflicts, purging all the other revisions.
	 *
	 * @param entity the winner entity.
	 * @param conflictsToPurge a list of revisions to purge
	 * @return a [ConflictResolutionResult] entity. It contains the saved winner entity with the updated revision and
	 * any conflicting revisions that still exists for the entity.
	 */
	suspend fun declareConflictWinner(entity: E, conflictsToPurge: List<String>): ConflictResolutionResult<E>

	/**
	 * Solves the conflicts of the entities managed by this service.
	 *
	 * @param limit the maximum number of conflicts to solve. If null, all conflicts will be solved.
	 * @param ids the ids of the entities to solve the conflicts for. If null, all entities will be treated.
	 *
	 * @return a [Flow] of [MergeResult] of the entities which have been updated.
	 */
	fun solveConflicts(limit: Int? = null, ids: List<String>? = null): Flow<MergeResult>
}
