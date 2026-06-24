package org.taktik.icure.asynclogic.impl.conflict

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.impl.ConflictResolutionLogicImpl
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.conflicts.MergeResult

/**
 * Implements the [org.taktik.icure.entities.conflicts.ConflictResolutionStrategy.LatestRevision] strategy.
 *
 * It does not attempt any merge: among all the conflicting revisions (including the current main one) it keeps the
 * revision with the greatest ordinal (the leading number of the CouchDB revision, e.g. `3` in `3-abc...`) and purges
 * all the others. If more than one revision shares the greatest ordinal a single winner cannot be chosen and the
 * resolution fails. The outcome is:
 * - [MergeResult.Success] if all the losing revisions were purged;
 * - [MergeResult.PartialSuccess] if only some of the losing revisions could be purged;
 * - [MergeResult.Failure] if no losing revision could be purged or a single winner could not be determined.
 */
object LatestRevisionConflictSolver : ConflictSolver {

	private fun String.ordinal(): Int = split("-").first().toIntOrNull() ?: 0

	context(logic: ConflictResolutionLogicImpl<E>)
	override suspend fun <E : StoredDocument> solve(
		entity: E,
		datastoreInformation: IDatastoreInformation
	): MergeResult {
		if (entity.conflicts == null || entity.conflicts!!.isEmpty()) {
			return MergeResult.Success(id = entity.id, rev = checkNotNull(entity.rev))
		}
		val revisions = (entity.conflicts ?: emptyList()) + checkNotNull(entity.rev)
		val winnerRev = revisions.maxBy { it.ordinal() }
		// If there are multiple revisions with the greatest ordinal, this strategy cannot choose
		if (revisions.filter { it.ordinal() == winnerRev.ordinal() }.size != 1) {
			MergeResult.Failure(id = entity.id)
		}
		val revsToBePurged = revisions.filter { it != winnerRev }
		val purgedRevs = if (revsToBePurged.isNotEmpty()) {
			logic.dao.purgeConflictingRevisions(
				datastoreInformation = datastoreInformation,
				entityId = entity.id,
				revisionsToPurge = revsToBePurged
			).filterSuccessfulUpdates().toList()
		} else {
			emptySet()
		}
		return when {
			revsToBePurged.size == purgedRevs.size -> MergeResult.Success(id = entity.id, rev = winnerRev)
			revsToBePurged.isNotEmpty() && purgedRevs.isEmpty() -> MergeResult.Failure(id = entity.id)
			else -> MergeResult.PartialSuccess(id = entity.id, rev = winnerRev)
		}
	}

}