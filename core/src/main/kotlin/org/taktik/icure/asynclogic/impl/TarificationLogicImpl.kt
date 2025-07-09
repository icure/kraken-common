/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import com.google.common.base.Preconditions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.TarificationDAO
import org.taktik.icure.asynclogic.TarificationLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Tarification
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

open class TarificationLogicImpl(
	protected val tarificationDAO: TarificationDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters
) : GenericLogicImpl<Tarification, TarificationDAO>(fixer, datastoreInstanceProvider, filters), TarificationLogic {

	override suspend fun getTarification(id: String): Tarification? {
		val datastoreInformation = getInstanceAndGroup()
		return tarificationDAO.get(datastoreInformation, id)
	}

	override suspend fun getTarification(type: String, tarification: String, version: String): Tarification? {
		val datastoreInformation = getInstanceAndGroup()
		return tarificationDAO.get(datastoreInformation, "$type|$tarification|$version")
	}

	override fun getTarifications(ids: List<String>): Flow<Tarification> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.getEntities(datastoreInformation, ids))
	}

	protected fun validateForCreation(batch: List<Tarification>) =
		batch.fold(setOf<Tarification>()) { acc, tarification ->    // First, I check that all the codes are valid
			if(tarification.rev != null) error("A new entity should not have a rev")
			tarification.code ?: error("Tarification field is null")
			tarification.type ?: error("Type field is null")
			tarification.version ?: error("Version field is null")

			if (acc.contains(tarification)) error("Batch contains duplicate elements. id: ${tarification.type}|${tarification.code}|${tarification.version}")

			acc + tarification.copy(id = tarification.type + "|" + tarification.code + "|" + tarification.version)
		}

	protected fun validateForModification(batch: List<Tarification>) =
		batch.fold(mapOf<String, Tarification>()) { acc, tarification -> // First, I check that all the codes are valid
			tarification.code ?: error("Tarification field is null")
			tarification.type ?: error("Type field is null")
			tarification.version ?: error("Version field is null")
			tarification.rev ?: error("rev field is null")

			if (tarification.id != "${tarification.type}|${tarification.code}|${tarification.version}") error("Tarification id does not match the tarification, type or version value")
			if (acc.contains(tarification.id)) error("The batch contains a duplicate")

			acc + (tarification.id to tarification)
		}.map {
				it.value
			}


	override suspend fun createTarification(tarification: Tarification) = fix(tarification, isCreate = true) { fixedTarification ->
		val datastoreInformation = getInstanceAndGroup()
		// assigning Tarification id type|tarification|version
		tarificationDAO.create(datastoreInformation, validateForCreation(listOf(fixedTarification)).first().copy(id = fixedTarification.type + "|" + fixedTarification.code + "|" + fixedTarification.version))
	}

	override suspend fun modifyTarification(tarification: Tarification) = fix(tarification, isCreate = false) { fixedTarification ->
		val datastoreInformation = getInstanceAndGroup()
		val existingTarification = fixedTarification.id.let { tarificationDAO.get(datastoreInformation, it) }
		Preconditions.checkState(existingTarification?.code == fixedTarification.code, "Modification failed. Tarification field is immutable.")
		Preconditions.checkState(existingTarification?.type == fixedTarification.type, "Modification failed. Type field is immutable.")
		Preconditions.checkState(existingTarification?.version == fixedTarification.version, "Modification failed. Version field is immutable.")
		modifyEntities(setOf(fixedTarification)).firstOrNull()
	}

	override fun findTarificationsBy(type: String?, tarification: String?, version: String?): Flow<Tarification> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.listTarificationsBy(datastoreInformation, type, tarification, version))
	}

	override fun findTarificationsBy(region: String?, type: String?, tarification: String?, version: String?): Flow<Tarification> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.listTarificationsBy(datastoreInformation, region, type, tarification, version))
	}

	override fun findTarificationsBy(
		region: String?,
		type: String?,
		tarification: String?,
		version: String?,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO
			.findTarificationsBy(datastoreInformation, region, type, tarification, version, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Tarification>(paginationOffset.limit)
		)
	}

	override fun findTarificationsOfTypesByLabel(
		region: String?,
		language: String?,
		label: String?,
		types: Set<String>?,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()

		suspend fun findTarificationsOfTypesByLabelRecursive(region: String?, language: String?, label: String?, types: Set<String>?, paginationOffset: PaginationOffset<ComplexKey>, extensionFactor: Float = 1f): Flow<ViewRowWithDoc<*, *, *>> {
			val offset = paginationOffset.copy(limit = (paginationOffset.limit * extensionFactor).toInt())
			var toEmit: ViewRowWithDoc<*, *, *>? = null
			var sentElements = 0
			var seenElements = 0

			return tarificationDAO
				.findTarificationsByLabel(datastoreInformation, region, language, label, offset)
				.transform {
					if(it is ViewRowWithDoc<*, *, *> && it.doc is Tarification) {
						if(toEmit != null) {
							emit(checkNotNull(toEmit))
							toEmit = null
						}
						if(types == null || types.contains((it.doc as Tarification).type)) {
							sentElements++
							toEmit = it
						}
						seenElements++
					}
				}.onCompletion {
					if(sentElements < paginationOffset.limit && sentElements < seenElements && toEmit != null) {
						emitAll(
							findTarificationsOfTypesByLabelRecursive(
								region, language, label, types,
								paginationOffset.copy(
									startKey = toEmit?.key as? ComplexKey,
									startDocumentId = toEmit?.id,
									limit = paginationOffset.limit - sentElements
								),
								(if (seenElements == 0) extensionFactor * 2 else (seenElements.toFloat() / sentElements)).coerceAtMost(100f)
							)
						)
					} else if(toEmit != null){
						emit(checkNotNull(toEmit))
					}
				}
		}

		emitAll(
			findTarificationsOfTypesByLabelRecursive(
				region,
				language,
				label,
				types,
				paginationOffset.limitIncludingKey(),
				100f
			).toPaginatedFlow<Tarification>(paginationOffset.limit)
		)
	}

	override fun findTarificationsByLabel(region: String?, language: String?, type: String?, label: String?, paginationOffset: PaginationOffset<List<String?>>): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.findTarificationsByLabel(datastoreInformation, region, language, type, label, paginationOffset))
	}

	override suspend fun getOrCreateTarification(type: String, tarification: String): Tarification? {
		val listTarifications = findTarificationsBy(type, tarification, null).toList()
		return listTarifications.takeIf { it.isNotEmpty() }?.let { it.sortedWith { a: Tarification, b: Tarification ->
			b.version!!.compareTo(
				a.version!!
			)
		}
		}?.first()
			?: createTarification(Tarification.from(type, tarification, "1.0"))
	}

	override fun solveConflicts(limit: Int?, ids: List<String>?) = flow { emitAll(doSolveConflicts(
		ids,
		limit,
		getInstanceAndGroup()
	)) }

	protected fun doSolveConflicts(
		ids: List<String>?,
		limit: Int?,
		datastoreInformation: IDatastoreInformation,
	) =  flow {
		val flow = ids?.asFlow()?.mapNotNull { tarificationDAO.get(datastoreInformation, it, Option.CONFLICTS) }
			?: tarificationDAO.listConflicts(datastoreInformation)
				.mapNotNull { tarificationDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { tarification ->
				tarification.conflicts?.mapNotNull { conflictingRevision ->
					tarificationDAO.get(
						datastoreInformation, tarification.id, conflictingRevision,
					)
				}?.fold(tarification to emptyList<Tarification>()) { (kept, toBePurged), conflict ->
					kept.merge(conflict) to toBePurged + conflict
				}?.let { (mergedCode, toBePurged) ->
					tarificationDAO.save(datastoreInformation, mergedCode).also {
						toBePurged.forEach {
							if (it.rev != null && it.rev != mergedCode.rev) {
								tarificationDAO.purge(datastoreInformation, listOf(it)).single()
							}
						}
					}
				}
			}
			.collect { emit(IdAndRev(it.id, it.rev)) }
	}

	override fun getGenericDAO(): TarificationDAO {
		return tarificationDAO
	}
}
