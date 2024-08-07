/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.MedicalLocation
import org.taktik.icure.pagination.PaginationElement

interface MedicalLocationService {
	suspend fun createMedicalLocation(medicalLocation: MedicalLocation): MedicalLocation?
	fun deleteMedicalLocations(ids: List<String>): Flow<DocIdentifier>
	suspend fun getMedicalLocation(medicalLocation: String): MedicalLocation?

	/**
	 * Retrieve one or more [MedicalLocation]s based on their [MedicalLocation.id]s.
	 *
	 * @param medicalLocationIds the ids of the Medical Locations to retrieve.
	 * @return a [Flow] of [MedicalLocation]s.
	 */
	fun getMedicalLocations(medicalLocationIds: List<String>): Flow<MedicalLocation>

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

	/**
	 * Retrieves the ids of the [MedicalLocation]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [MedicalLocation].
	 * @param deduplicate whether to remove the duplicate ids from the result, if any.
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the current user does not have the Permission to search medical locations with a filter.
	 */
	fun matchMedicalLocationsBy(filter: AbstractFilter<MedicalLocation>, deduplicate: Boolean): Flow<String>
}
