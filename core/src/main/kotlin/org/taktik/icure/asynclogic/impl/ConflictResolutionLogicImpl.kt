package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import org.taktik.couchdb.entity.Option
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncdao.ConflictDAO
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.ConflictResolutionLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.conflicts.ConflictResolutionResult
import org.taktik.icure.entities.conflicts.MergeResult
import org.taktik.icure.mergers.Merger

open class ConflictResolutionLogicImpl<E : StoredDocument>(
	private val dao: ConflictDAO<E>,
	private val merger: Merger<E>,
	private val datastoreInstanceProvider: DatastoreInstanceProvider
) : ConflictResolutionLogic {

	// TODO return only ids
	protected fun doGetConflictingEntities(
		datastoreInformation: IDatastoreInformation
	): Flow<E> = dao.listConflicts(datastoreInformation)

	protected fun doGetConflictsFor(
		id: String,
		datastoreInformation: IDatastoreInformation,
	): Flow<E> = flow {
		val entity = dao.get(datastoreInformation, id, Option.CONFLICTS)
			?: throw DocumentNotFoundException("Entity with id $id not found")
		if (entity.conflicts.isNullOrEmpty()) {
			throw IllegalArgumentException("Entity with id $id has no conflicts")
		}
		emit(entity)
		emitAll(
			entity.conflicts?.asFlow()?.mapNotNull { conflictingRevision ->
				dao.get(
					datastoreInformation,
					entity.id,
					conflictingRevision,
				)
			} ?: emptyFlow()
		)
	}

	protected suspend fun doDeclareConflictWinner(
		entity: E,
		conflictsToPurge: List<String>,
		datastoreInformation: IDatastoreInformation,
	): ConflictResolutionResult<E> {
		val savedWinner = dao.save(datastoreInformation, entity)
		dao.purgeConflictingRevisions(
			datastoreInformation,
			entityId = savedWinner.id,
			revisionsToPurge = conflictsToPurge
		).collect()
		val remainingConflicts = dao.get(datastoreInformation, savedWinner.id, Option.CONFLICTS)
		return ConflictResolutionResult(
			document = savedWinner,
			remainingConflicts = (setOfNotNull(
				remainingConflicts?.rev,
				*(remainingConflicts?.conflicts ?: emptyList()).toTypedArray()
			) - savedWinner.rev!!).toList()
		)
	}

	protected fun doSolveConflicts(
		ids: List<String>?,
		limit: Int?,
		datastoreInformation: IDatastoreInformation,
	): Flow<MergeResult> = flow {
		val flow = ids?.asFlow()?.mapNotNull { dao.get(datastoreInformation, it, Option.CONFLICTS) }
				?: dao.listConflicts(datastoreInformation)
					.mapNotNull { dao.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow).collect { entity ->
			val mutableConflicts = entity.conflicts?.toMutableList() ?: mutableListOf()
			val toBePurged = mutableListOf<E>()
			var merged = entity
			while (mutableConflicts.isNotEmpty()) {
				// Remove from the conflicts only after checking that the entity with that rev exists and is mergeable,
				// to avoid mixing up the PartialSuccess and Failure cases.
				val conflictingRevision = mutableConflicts.first()
				val conflict = dao.get(
					datastoreInformation,
					entity.id,
					conflictingRevision,
				)
				when {
					conflict != null && merger.canMerge(merged, conflict) -> {
						merged = merger.merge(merged, conflict)
						toBePurged.add(conflict)
						mutableConflicts.removeAt(0)
					}
					conflict != null && !merger.canMerge(merged, conflict) -> break
					else -> {
						mutableConflicts.removeAt(0)
					}
				}
			}
			val entityAfterMerge = if (toBePurged.isNotEmpty()) {
				dao.save(datastoreInformation, merged).also {
					TODO()
					dao.purge(datastoreInformation, toBePurged).filterSuccessfulUpdates().count() != toBePurged.size
				}
			} else entity
			when {
				mutableConflicts.isEmpty() -> MergeResult.Success(
					id = entityAfterMerge.id,
					rev = checkNotNull(entityAfterMerge.rev) { "Merged entity must have a revision" },
				)
				mutableConflicts.size != (entity.conflicts?.size ?: 0) -> MergeResult.PartialSuccess(
					id = entityAfterMerge.id,
					rev = checkNotNull(entityAfterMerge.rev) { "Merged entity must have a revision" },
				)
				else -> MergeResult.Failure(id = entity.id)
			}.also {
				emit(it)
			}
		}
	}

	override fun solveConflicts(
		limit: Int?,
		ids: List<String>?,
	): Flow<MergeResult> = flow {
		emitAll(
			doSolveConflicts(
				ids,
				limit,
				datastoreInstanceProvider.getInstanceAndGroup(),
			),
		)
	}
}