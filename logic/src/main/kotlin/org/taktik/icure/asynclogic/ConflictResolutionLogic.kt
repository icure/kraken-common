package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.conflicts.MergeResult

interface ConflictResolutionLogic {
	fun solveConflicts(limit: Int?, ids: List<String>?): Flow<MergeResult>
}