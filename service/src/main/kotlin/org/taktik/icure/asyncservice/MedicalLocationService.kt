/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.MedicalLocation
import org.taktik.icure.pagination.PaginationElement

interface MedicalLocationService {
	suspend fun createMedicalLocation(medicalLocation: MedicalLocation): MedicalLocation?

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [MedicalLocation]s.
	 */
	fun deleteMedicalLocations(ids: List<IdAndRev>): Flow<MedicalLocation>

//    /**
//     * Marks an entity as deleted.
//     * The data of the entity is preserved, but the entity won't appear in most queries.
//     *
//     * @param id the id of the entity to delete.
//     * @param rev
//     * @return the updated [DocIdentifier] for the entity.
//     * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
//     * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
//     * @throws ConflictRequestException if the entity rev doesn't match.
//     */
//    suspend fun deleteMedicalLocation(id: String, rev: String?): DocIdentifier
//
//    /**
//     * Deletes an entity.
//     * An entity deleted this way can't be restored.
//     * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
//     *
//     * @param id the id of the entity
//     * @param rev the latest known revision of the entity.
//     * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
//     * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
//     * @throws ConflictRequestException if the entity rev doesn't match.
//     */
//    suspend fun purgeMedicalLocation(id: String, rev: String): DocIdentifier
//
//    /**
//     * Restores an entity marked as deleted.
//     * The user needs to have write access to the entity
//     * @param id the id of the entity marked to restore
//     * @param rev the revision of the entity after it was marked as deleted
//     * @return the restored entity
//     */
//    suspend fun undeleteMedicalLocation(id: String, rev: String): MedicalLocation
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
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the current user does not have the Permission to search medical locations with a filter.
	 */
	fun matchMedicalLocationsBy(filter: AbstractFilter<MedicalLocation>): Flow<String>
}
