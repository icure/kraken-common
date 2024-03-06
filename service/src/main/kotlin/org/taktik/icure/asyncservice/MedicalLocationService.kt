/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.MedicalLocation
import org.taktik.icure.pagination.PaginationElement

interface MedicalLocationService {
	suspend fun createMedicalLocation(medicalLocation: MedicalLocation): MedicalLocation?
	fun deleteMedicalLocations(ids: List<String>): Flow<DocIdentifier>
	suspend fun getMedicalLocation(medicalLocation: String): MedicalLocation?
	suspend fun modifyMedicalLocation(medicalLocation: MedicalLocation): MedicalLocation?
	fun findMedicalLocationByPostCode(postCode: String): Flow<MedicalLocation>

	/**
	 * Retrieves all the [MedicalLocation]s in a group in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with a key that is always null) for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [MedicalLocation]s
	 */
	fun getAllMedicalLocations(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement>

	/**
	 * Retrieves all the [MedicalLocation]s in a group.
	 *
	 * @return a [Flow] of [MedicalLocation]s
	 */
	fun getAllMedicalLocations(): Flow<MedicalLocation>
}
