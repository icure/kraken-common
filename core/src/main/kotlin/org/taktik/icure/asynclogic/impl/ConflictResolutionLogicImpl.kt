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
		val entity = dao.getBypassingCache(datastoreInformation, id = id, rev = null, Option.CONFLICTS)
			?: throw DocumentNotFoundException("Entity with id $id not found")
		if (entity.conflicts.isNullOrEmpty()) {
			throw IllegalArgumentException("Entity with id $id has no conflicts")
		}
		emit(entity)
		emitAll(
			entity.conflicts?.asFlow()?.mapNotNull { conflictingRevision ->
				dao.getBypassingCache(
					datastoreInformation = datastoreInformation,
					id = entity.id,
					rev = conflictingRevision,
				)
			} ?: emptyFlow()
		)
	}

	/**
	 * Ensures that the systemMetadata of the entity (if any) that the user declared as winner are equal to the ones of
	 * the current main revision of the entity. Any conflict on systemMetadata will be merged automatically by the
	 * declare winner method.
	 */
	private suspend fun requireUnmodifiedSecurityMetadata(
		entity: E,
		datastoreInformation: IDatastoreInformation,
	) {
		if (entity is HasEncryptionMetadata) {
			val currentRevisionOnDb = dao.getBypassingCache(
				datastoreInformation = datastoreInformation,
				id = entity.id,
				rev = entity.rev
			)?.let {
				it as HasEncryptionMetadata
			} ?: throw NotFoundRequestException("Entity with id ${entity.id} with rev ${entity.rev} not found")
			if (!entity.encryptableMetadataEquals(currentRevisionOnDb)) {
				throw IllegalArgumentException("SystemMetadata of winner is not equal to the one in the database")
			}
		}
	}

	private fun mergeSecurityMetadataOfOldRevisions(
		entity: E,
		oldRevisions: List<E>
	): E =
		if (entity !is HasEncryptionMetadata) {
			entity
		} else {
			mergeSecurityMetadataOfOldRevisionsForEncryptable(entity, oldRevisions)
		}

	private fun mergeSecurityMetadataOfOldRevisionsForEncryptable(
		entity: E,
		oldRevisions: List<E>
	): E = oldRevisions.fold(entity) { acc, conflict ->
		val accWithMetadata = acc as HasEncryptionMetadata
		if (conflict is HasEncryptionMetadata && !accWithMetadata.encryptableMetadataEquals(conflict)) {
			@Suppress("UNCHECKED_CAST")
			accWithMetadata.withEncryptionMetadata(
				securityMetadata =
					accWithMetadata.securityMetadata?.let { intoMetadata ->
						conflict.securityMetadata?.let { fromMetadata ->
							intoMetadata.mergeForDifferentVersionsOfEntity(fromMetadata)
						} ?: intoMetadata
					} ?: conflict.securityMetadata,
				delegations = MergeUtil.mergeMapsOfSets(conflict.delegations, accWithMetadata.delegations),
				encryptionKeys = accWithMetadata.encryptionKeys,
				cryptedForeignKeys = MergeUtil.mergeMapsOfSets(conflict.cryptedForeignKeys, accWithMetadata.cryptedForeignKeys),
				secretForeignKeys = conflict.secretForeignKeys + accWithMetadata.secretForeignKeys,
			) as E
		} else {
			acc
		}
	}

	protected suspend fun doDeclareConflictWinner(
		entity: E,
		conflictsToPurge: List<E>,
		datastoreInformation: IDatastoreInformation,
	): ConflictResolutionResult<E> {
		requireUnmodifiedSecurityMetadata(entity, datastoreInformation)
		val savedWinner = dao.save(
			datastoreInformation = datastoreInformation,
			entity = mergeSecurityMetadataOfOldRevisions(entity = entity, oldRevisions = conflictsToPurge),
		)
		dao.purgeConflictingRevisions(
			datastoreInformation,
			entityId = savedWinner.id,
			revisionsToPurge = conflictsToPurge.mapNotNull { it.rev }
		).collect()
		val remainingConflicts = dao.getBypassingCache(datastoreInformation, id = savedWinner.id, rev = null,Option.CONFLICTS)
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
					.mapNotNull { dao.getBypassingCache(datastoreInformation, id = it.id, rev = null, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow).collect { entity ->
			val mutableConflicts = entity.conflicts?.toMutableList() ?: mutableListOf()
			val toBePurged = mutableListOf<E>()
			var merged = entity
			while (mutableConflicts.isNotEmpty()) {
				// Remove from the conflicts only after checking that the entity with that rev exists and is mergeable,
				// to avoid mixing up the PartialSuccess and Failure cases.
				val conflictingRevision = mutableConflicts.first()
				val conflict = dao.getBypassingCache(
					datastoreInformation = datastoreInformation,
					id = entity.id,
					rev = conflictingRevision,
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

	override suspend fun declareConflictWinner(entity: E, conflictsToPurge: List<E>) =
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

	override suspend fun getBypassingCache(id: String, rev: String): E? =
		dao.getBypassingCache(
			datastoreInformation = datastoreInstanceProvider.getInstanceAndGroup(),
			id = id,
			rev = rev
		)
}