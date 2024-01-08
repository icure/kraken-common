/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import com.google.common.base.Preconditions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.TarificationDAO
import org.taktik.icure.asynclogic.TarificationLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Tarification
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class TarificationLogicImpl(
	private val tarificationDAO: TarificationDAO,
	datastoreInstanceProvider: org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider,
	fixer: Fixer
) : GenericLogicImpl<Tarification, TarificationDAO>(fixer, datastoreInstanceProvider), TarificationLogic {

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

	override suspend fun createTarification(tarification: Tarification) = fix(tarification) { fixedTarification ->
		if(fixedTarification.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		fixedTarification.code ?: error("Code field is null")
		fixedTarification.type ?: error("Type field is null")
		fixedTarification.version ?: error("Version field is null")

		val datastoreInformation = getInstanceAndGroup()
		// assigning Tarification id type|tarification|version
		tarificationDAO.create(datastoreInformation, fixedTarification.copy(id = fixedTarification.type + "|" + fixedTarification.code + "|" + fixedTarification.version))
	}

	override suspend fun modifyTarification(tarification: Tarification) = fix(tarification) { fixedTarification ->
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
		paginationOffset: PaginationOffset<List<String?>>
	): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.findTarificationsBy(datastoreInformation, region, type, tarification, version, paginationOffset))
	}

	override fun findTarificationsByLabel(region: String?, language: String?, label: String?, paginationOffset: PaginationOffset<List<String?>>): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(tarificationDAO.findTarificationsByLabel(datastoreInformation, region, language, label, paginationOffset))
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

	override fun getGenericDAO(): TarificationDAO {
		return tarificationDAO
	}
}
