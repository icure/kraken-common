/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.taktik.icure.asyncdao.MedicalLocationDAO
import org.taktik.icure.asynclogic.MedicalLocationLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.MedicalLocation
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

open class MedicalLocationLogicImpl(
	private val medicalLocationDAO: MedicalLocationDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters
) : GenericLogicImpl<MedicalLocation, MedicalLocationDAO>(fixer, datastoreInstanceProvider, filters), MedicalLocationLogic {

	override suspend fun createMedicalLocation(medicalLocation: MedicalLocation) = fix(medicalLocation, isCreate = true) { fixedMedicalLocation ->
		if(fixedMedicalLocation.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		val datastoreInformation = getInstanceAndGroup()
		medicalLocationDAO.create(datastoreInformation, fixedMedicalLocation)
	}

	override fun getAllMedicalLocations(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(medicalLocationDAO
			.getAllPaginated(datastoreInformation, paginationOffset.limitIncludingKey(), Nothing::class.java)
			.toPaginatedFlow<MedicalLocation>(paginationOffset.limit)
		)
	}

	override suspend fun getMedicalLocation(medicalLocation: String): MedicalLocation? {
		val datastoreInformation = getInstanceAndGroup()
		return medicalLocationDAO.get(datastoreInformation, medicalLocation)
	}

	override suspend fun modifyMedicalLocation(medicalLocation: MedicalLocation) = fix(medicalLocation, isCreate = false) { fixedMedicalLocation ->
		val datastoreInformation = getInstanceAndGroup()
		medicalLocationDAO.save(datastoreInformation, fixedMedicalLocation)
	}

	override fun findMedicalLocationByPostCode(postCode: String): Flow<MedicalLocation> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(medicalLocationDAO.byPostCode(datastoreInformation, postCode))
	}

	override fun getGenericDAO(): MedicalLocationDAO {
		return medicalLocationDAO
	}
}
