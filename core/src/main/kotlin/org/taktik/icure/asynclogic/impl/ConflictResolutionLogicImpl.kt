package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.ConflictDAO
import org.taktik.icure.asynclogic.ConflictResolutionLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.mergers.Merger

open class ConflictResolutionLogicImpl<E : StoredDocument>(
	private val dao: ConflictDAO<E>,
	private val merger: Merger<E>,
	private val datastoreInstanceProvider: DatastoreInstanceProvider
) : ConflictResolutionLogic {

	protected fun doSolveConflicts(
		ids: List<String>?,
		limit: Int?,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		val flow = ids?.asFlow()?.mapNotNull { dao.get(datastoreInformation, it, Option.CONFLICTS) }
				?: dao
					.listConflicts(datastoreInformation)
					.mapNotNull { dao.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow).mapNotNull { entity ->
			entity.conflicts
				?.mapNotNull { conflictingRevision ->
					dao.get(
						datastoreInformation,
						entity.id,
						conflictingRevision,
					)
				}?.fold(entity to emptyList<E>()) { (kept, toBePurged), conflict ->
					if (merger.canMerge(kept, conflict)) {
						merger.merge(kept, conflict) to (toBePurged + conflict)
					} else {
						kept to toBePurged
					}
				}?.let { (mergedCode, toBePurged) ->
					dao.save(datastoreInformation, mergedCode).also {
						toBePurged.forEach {
							if (it.rev != null && it.rev != mergedCode.rev) {
								dao.purge(datastoreInformation, listOf(it)).single()
							}
						}
					}
				}
		}.collect { emit(IdAndRev(it.id, it.rev)) }
	}

	override fun solveConflicts(
		limit: Int?,
		ids: List<String>?,
	): Flow<IdAndRev> = flow {
		emitAll(
			doSolveConflicts(
				ids,
				limit,
				datastoreInstanceProvider.getInstanceAndGroup(),
			),
		)
	}
}