/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import com.google.common.base.Preconditions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncdao.TarificationDAO
import org.taktik.icure.asynclogic.TarificationLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Tarification
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

open class TarificationLogicImpl(
	val tarificationDAO: TarificationDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : GenericLogicImpl<Tarification, TarificationDAO>(fixer, datastoreInstanceProvider, filters),
	TarificationLogic {

	private fun validateIdFields(code: Tarification) {
		requireNotNull(code.code) { "Element with id ${code.id} has a null code field." }
		requireNotNull(code.type) { "Element with id ${code.id} has a null type field." }
		requireNotNull(code.version) { "Element with id ${code.id} has a null version field." }
	}

	protected fun validateForCreation(batch: List<Tarification>) = batch.fold(setOf<Tarification>()) { acc, code ->
		validateIdFields(code)
		acc + code.copy(id = code.type + "|" + code.code + "|" + code.version)
	}

	protected fun validateForModification(batch: List<Tarification>) = batch
		.fold(mapOf<String, Tarification>()) { acc, code ->
			validateIdFields(code)
			requireNotNull(code.rev) { "Element with id ${code.id} has a null rev field. Rev must be non-null when modifying an entity" }

			require(code.id == "${code.type}|${code.code}|${code.version}") {
				error("Element with id ${code.id} has an id that does not match the code, type or version value: id must be equal to type|code|version")
			}

			acc + (code.id to code)
		}.map {
			it.value
		}

	override suspend fun getTarification(id: String): Tarification? {
		val datastoreInformation = getInstanceAndGroup()
		return tarificationDAO.get(datastoreInformation, id)
	}

	override suspend fun getTarification(
		type: String,
		tarification: String,
		version: String,
	): Tarification? {
		val datastoreInformation = getInstanceAndGroup()
		return tarificationDAO.get(datastoreInformation, "$type|$tarification|$version")
	}

	override fun getTarifications(ids: List<String>): Flow<Tarification> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.getEntities(datastoreInformation, ids))
	}

	override suspend fun createTarification(tarification: Tarification) = fix(tarification, isCreate = true) { fixedTarification ->
		if (fixedTarification.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		validateIdFields(fixedTarification)

		val datastoreInformation = getInstanceAndGroup()
		// assigning Tarification id type|tarification|version
		tarificationDAO.create(
			datastoreInformation,
			fixedTarification.copy(
				id =
				fixedTarification.type + "|" + fixedTarification.code + "|" + fixedTarification.version,
			),
		)
	}

	override suspend fun modifyTarification(tarification: Tarification) = fix(tarification, isCreate = false) { fixedTarification ->
		val datastoreInformation = getInstanceAndGroup()
		val existingTarification = fixedTarification.id.let { tarificationDAO.get(datastoreInformation, it) }
		Preconditions.checkState(existingTarification?.code == fixedTarification.code, "Modification failed. Tarification field is immutable.")
		Preconditions.checkState(existingTarification?.type == fixedTarification.type, "Modification failed. Type field is immutable.")
		Preconditions.checkState(existingTarification?.version == fixedTarification.version, "Modification failed. Version field is immutable.")
		modifyEntities(setOf(fixedTarification)).firstOrNull()
	}

	override fun findTarificationsBy(
		type: String?,
		tarification: String?,
		version: String?,
	): Flow<Tarification> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.listTarificationsBy(datastoreInformation, type, tarification, version))
	}

	override fun findTarificationsBy(
		region: String?,
		type: String?,
		tarification: String?,
		version: String?,
	): Flow<Tarification> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.listTarificationsBy(datastoreInformation, region, type, tarification, version))
	}

	override fun findTarificationsBy(
		region: String?,
		type: String?,
		tarification: String?,
		version: String?,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			tarificationDAO
				.findTarificationsBy(datastoreInformation, region, type, tarification, version, paginationOffset.limitIncludingKey())
				.toPaginatedFlow<Tarification>(paginationOffset.limit),
		)
	}

	override fun findTarificationsOfTypesByLabel(
		region: String?,
		language: String?,
		label: String?,
		types: Set<String>?,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()

		suspend fun findTarificationsOfTypesByLabelRecursive(
			region: String?,
			language: String?,
			label: String?,
			types: Set<String>?,
			paginationOffset: PaginationOffset<ComplexKey>,
			extensionFactor: Float = 1f,
		): Flow<ViewRowWithDoc<*, *, *>> {
			val offset = paginationOffset.copy(limit = (paginationOffset.limit * extensionFactor).toInt())
			var toEmit: ViewRowWithDoc<*, *, *>? = null
			var sentElements = 0
			var seenElements = 0

			return tarificationDAO
				.findTarificationsByLabel(datastoreInformation, region, language, label, offset)
				.transform {
					if (it is ViewRowWithDoc<*, *, *> && it.doc is Tarification) {
						if (toEmit != null) {
							emit(checkNotNull(toEmit))
							toEmit = null
						}
						if (types == null || types.contains((it.doc as Tarification).type)) {
							sentElements++
							toEmit = it
						}
						seenElements++
					}
				}.onCompletion {
					if (sentElements < paginationOffset.limit && sentElements < seenElements && toEmit != null) {
						emitAll(
							findTarificationsOfTypesByLabelRecursive(
								region,
								language,
								label,
								types,
								paginationOffset.copy(
									startKey = toEmit?.key as? ComplexKey,
									startDocumentId = toEmit?.id,
									limit = paginationOffset.limit - sentElements,
								),
								(if (seenElements == 0) extensionFactor * 2 else (seenElements.toFloat() / sentElements)).coerceAtMost(100f),
							),
						)
					} else if (toEmit != null) {
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
				100f,
			).toPaginatedFlow<Tarification>(paginationOffset.limit),
		)
	}

	override fun findTarificationsByLabel(
		region: String?,
		language: String?,
		type: String?,
		label: String?,
		paginationOffset: PaginationOffset<List<String?>>,
	): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.findTarificationsByLabel(datastoreInformation, region, language, type, label, paginationOffset))
	}

	override suspend fun getOrCreateTarification(
		type: String,
		tarification: String,
	): Tarification? {
		val listTarifications = findTarificationsBy(type, tarification, null).toList()
		return listTarifications
			.takeIf { it.isNotEmpty() }
			?.let {
				it.sortedWith { a: Tarification, b: Tarification ->
					b.version!!.compareTo(
						a.version!!,
					)
				}
			}?.first()
			?: createTarification(Tarification.from(type, tarification, "1.0"))
	}

	override fun getGenericDAO(): TarificationDAO = tarificationDAO
}
