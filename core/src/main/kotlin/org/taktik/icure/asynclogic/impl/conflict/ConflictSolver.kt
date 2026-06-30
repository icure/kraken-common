package org.taktik.icure.asynclogic.impl.conflict

import org.taktik.icure.asynclogic.impl.ConflictResolutionLogicImpl
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.conflicts.ConflictResolutionStrategy
import org.taktik.icure.entities.conflicts.MergeResult

/**
 * A [ConflictSolver] encapsulates the logic to automatically resolve the conflicting revisions of a [StoredDocument].
 *
 * Each [ConflictResolutionStrategy] is mapped to a concrete [ConflictSolver] implementation through
 * [getConflictSolver]. Implementations are stateless singletons (objects) and receive the
 * [ConflictResolutionLogicImpl] that requested the resolution as a context receiver, so they can access the dao and
 * the merger of the entity being solved.
 */
interface ConflictSolver {
	/**
	 * Resolves the conflicting revisions of the provided [entity].
	 *
	 * @param entity the entity to solve, retrieved including its conflicting revisions (see [org.taktik.couchdb.entity.Option.CONFLICTS]).
	 * @param datastoreInformation the information about the datastore where the entity is stored.
	 * @return a [MergeResult] describing the outcome of the resolution (success, partial success or failure).
	 */
	context(logic: ConflictResolutionLogicImpl<E>)
	suspend fun <E : StoredDocument> solve(entity: E, datastoreInformation: IDatastoreInformation): MergeResult
}

/**
 * Maps a [ConflictResolutionStrategy] to the [ConflictSolver] that implements it.
 */
fun ConflictResolutionStrategy.getConflictSolver(): ConflictSolver = when (this) {
	ConflictResolutionStrategy.FullMergeability -> FullMergeabilityConflictSolver
	ConflictResolutionStrategy.LatestRevision -> LatestRevisionConflictSolver
}