package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.conflicts.ConflictResolutionResult
import org.taktik.icure.entities.conflicts.MergeResult

interface ConflictResolutionLogic<E : StoredDocument> {
	fun getConflictingEntitiesIds(): Flow<String>
	fun getConflictsFor(entityId: String): Flow<E>
	suspend fun declareConflictWinner(entity: E, conflictsToPurge: List<String>): ConflictResolutionResult<E>
	fun solveConflicts(limit: Int?, ids: List<String>?): Flow<MergeResult>
}