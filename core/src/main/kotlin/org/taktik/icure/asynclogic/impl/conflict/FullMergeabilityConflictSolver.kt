package org.taktik.icure.asynclogic.impl.conflict

import kotlinx.coroutines.flow.count
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.impl.ConflictResolutionLogicImpl
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.conflicts.MergeResult

/**
 * Implements the [org.taktik.icure.entities.conflicts.ConflictResolutionStrategy.FullMergeability] strategy.
 *
 * It iterates over the conflicting revisions of the entity and, for each one that the entity-specific merger declares
 * mergeable with the accumulator, merges it into the winner and schedules the conflicting revision for purge. Revisions
 * that cannot be merged are left untouched. The outcome is:
 * - [MergeResult.Success] if every conflicting revision was merged and purged;
 * - [MergeResult.PartialSuccess] if at least one (but not all) revision was merged and the merged revisions were purged;
 * - [MergeResult.Failure] otherwise.
 */
object FullMergeabilityConflictSolver : ConflictSolver{
	context(logic: ConflictResolutionLogicImpl<E>)
	override suspend fun <E : StoredDocument> solve(entity: E, datastoreInformation: IDatastoreInformation): MergeResult {
		val mutableConflicts = entity.conflicts?.toMutableList() ?: mutableListOf()
		val toBePurged = mutableListOf<E>()
		var merged = entity
		while (mutableConflicts.isNotEmpty()) {
			// Remove from the conflicts only after checking that the entity with that rev exists and is mergeable,
			// to avoid mixing up the PartialSuccess and Failure cases.
			val conflictingRevision = mutableConflicts.first()
			val conflict = logic.dao.getBypassingCache(
				datastoreInformation = datastoreInformation,
				id = entity.id,
				rev = conflictingRevision,
			)
			when {
				conflict != null && logic.merger.canMerge(merged, conflict) -> {
					merged = logic.merger.merge(merged, conflict)
					toBePurged.add(conflict)
					mutableConflicts.removeAt(0)
				}
				conflict != null && !logic.merger.canMerge(merged, conflict) -> break
				else -> {
					mutableConflicts.removeAt(0)
				}
			}
		}
		val (entityAfterMerge, purgedCount) = if (toBePurged.isNotEmpty()) {
			logic.dao.save(datastoreInformation, merged) to
				logic.dao.purge(datastoreInformation, toBePurged).filterSuccessfulUpdates().count()
		} else {
			entity to 0
		}
		return when {
			// All conflicts have been merged and all the other revision have been purged
			mutableConflicts.isEmpty() && purgedCount == toBePurged.size -> MergeResult.Success(
				id = entityAfterMerge.id,
				rev = checkNotNull(entityAfterMerge.rev) { "Merged entity must have a revision" },
			)
			// There are unmergeable conflicts, but the revisions of merged conflicts have been successfully purged
			mutableConflicts.size != (entity.conflicts?.size ?: 0) && purgedCount == toBePurged.size -> MergeResult.PartialSuccess(
				id = entityAfterMerge.id,
				rev = checkNotNull(entityAfterMerge.rev) { "Merged entity must have a revision" },
			)
			else -> MergeResult.Failure(id = entityAfterMerge.id)
		}
	}
}