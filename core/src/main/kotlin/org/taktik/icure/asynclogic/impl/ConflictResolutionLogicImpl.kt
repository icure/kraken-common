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
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.base.encryptableMetadataEquals
import org.taktik.icure.entities.conflicts.ConflictResolutionResult
import org.taktik.icure.entities.conflicts.MergeResult
import org.taktik.icure.entities.utils.MergeUtil
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.mergers.Merger

open class ConflictResolutionLogicImpl<E : StoredDocument>(
	private val dao: ConflictDAO<E>,
	private val merger: Merger<E>,
	private val datastoreInstanceProvider: DatastoreInstanceProvider
) : ConflictResolutionLogic<E> {

	protected fun doGetConflictingEntitiesIds(
		datastoreInformation: IDatastoreInformation
	): Flow<String> = dao.listIdsOfEntitiesWithConflicts(datastoreInformation)

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

	private suspend fun requireUnmodifiedSecurityMetadata(
		entity: E,
		datastoreInformation: IDatastoreInformation,
	) {
		if (entity is HasEncryptionMetadata) {
			val currentRevisionOnDb = dao.get(datastoreInformation, entity.id, entity.rev)?.let {
				it as HasEncryptionMetadata
			} ?: throw NotFoundRequestException("Entity with id ${entity.id} with rev ${entity.rev} not found")
			if (!entity.encryptableMetadataEquals(currentRevisionOnDb)) {
				throw IllegalArgumentException("SystemMetadata of winner is not equal to the one in the database")
			}
		}
	}

	private suspend fun mergeSecurityMetadataOfOldRevisions(
		entity: E,
		conflictsToPurge: List<String>,
		datastoreInformation: IDatastoreInformation,
	): E =
		if (entity is HasEncryptionMetadata) {
			conflictsToPurge.fold(entity) { acc, rev ->
				val conflict = dao.get(datastoreInformation, entity.id, rev)?.let {
					it as HasEncryptionMetadata
				}
				if (conflict != null && !acc.encryptableMetadataEquals(conflict)) {
					val res = acc.withEncryptionMetadata(
						securityMetadata =
							acc.securityMetadata?.let { intoMetadata ->
								conflict.securityMetadata?.let { fromMetadata ->
									intoMetadata.mergeForDifferentVersionsOfEntity(fromMetadata)
								} ?: intoMetadata
							} ?: conflict.securityMetadata,
						delegations = MergeUtil.mergeMapsOfSets(conflict.delegations, acc.delegations),
						encryptionKeys = acc.encryptionKeys,
						cryptedForeignKeys = MergeUtil.mergeMapsOfSets(conflict.cryptedForeignKeys, acc.cryptedForeignKeys),
						secretForeignKeys = conflict.secretForeignKeys + acc.secretForeignKeys,
					) as E
					acc
				} else {
					acc
				}
			}
		} else {
			entity
		}

	protected suspend fun doDeclareConflictWinner(
		entity: E,
		conflictsToPurge: List<String>,
		datastoreInformation: IDatastoreInformation,
	): ConflictResolutionResult<E> {
		requireUnmodifiedSecurityMetadata(entity, datastoreInformation)
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
			val (entityAfterMerge, purgedCount) = if (toBePurged.isNotEmpty()) {
				dao.save(datastoreInformation, merged) to
					dao.purge(datastoreInformation, toBePurged).filterSuccessfulUpdates().count()
			} else {
				entity to 0
			}
			when {
				// All conflicts have been merged and all the other revision have benn purged
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
			}.also {
				emit(it)
			}
		}
	}

	override fun getConflictingEntitiesIds(): Flow<String> = flow {
		emitAll(
			doGetConflictingEntitiesIds(datastoreInstanceProvider.getInstanceAndGroup())
		)
	}

	override fun getConflictsFor(entityId: String): Flow<E> = flow {
		emitAll(
			doGetConflictsFor(entityId, datastoreInstanceProvider.getInstanceAndGroup())
		)
	}

	override suspend fun declareConflictWinner(entity: E, conflictsToPurge: List<String>) =
		doDeclareConflictWinner(entity, conflictsToPurge, datastoreInstanceProvider.getInstanceAndGroup())

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