package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.entity.IdAndRev

interface ConflictResolutionLogic {
	fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev>
}