package org.taktik.icure.asyncservice.base

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.entities.base.StoredDocument

/**
 * Defines methods which should be common to the logic of all [StoredDocument] entities for which conflicts resolution should be triggered through the API.
 */
interface EntityWithConflictResolutionService {
	/**
	 * Solves the conflicts of the entities managed by this service.
	 *
	 * @param limit the maximum number of conflicts to solve. If null, all conflicts will be solved.
	 * @param ids the ids of the entities to solve the conflicts for. If null, all entities will be treated.
	 *
	 * @return a [Flow] of [IdAndRev] of the entities which have been updated.
	 */
	fun solveConflicts(limit: Int? = null, ids: List<String>? = null): Flow<IdAndRev>
}
