/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Insurance
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface InsuranceService {
	suspend fun createInsurance(insurance: Insurance): Insurance?

	/**
//     * Marks a batch of entities as deleted.
//     * The data of the entities is preserved, but they won't appear in most queries.
//     * Ignores entities that:
//     * - don't exist
//     * - the user can't delete due to limited lack of write access
//     * - don't match the provided revision (if provided)
//     *
//     * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
//     * @return a [Flow] containing the [DocIdentifier]s of the entities successfully deleted.
//     */
//    fun deleteInsurances(ids: List<IdAndRev>): Flow<DocIdentifier>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param insuranceId the id of the entity to delete.
	 * @param rev the latest rev of the entity to delete.
	 * @return the deleted [Insurance].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [insuranceId] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteInsurance(insuranceId: String, rev: String?): Insurance

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
//    suspend fun purgeInsurance(id: String, rev: String): DocIdentifier
//
//    /**
//     * Restores an entity marked as deleted.
//     * The user needs to have write access to the entity
//     * @param id the id of the entity marked to restore
//     * @param rev the revision of the entity after it was marked as deleted
//     * @return the restored entity
//     */
//    suspend fun undeleteInsurance(id: String, rev: String): Insurance
	suspend fun getInsurance(insuranceId: String): Insurance?
	fun listInsurancesByCode(code: String): Flow<Insurance>
	fun listInsurancesByName(name: String): Flow<Insurance>

	suspend fun modifyInsurance(insurance: Insurance): Insurance?
	fun getInsurances(ids: Set<String>): Flow<Insurance>

	/**
	 * Retrieves all the [Insurance]s defined in the group of the current logged-in user in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for pagination.
	 * @return a [Flow] of [Insurance]s wrapped in [PaginationElement]s for pagination.
	 * @throws AccessDeniedException if the user is not an admin or a healthcare party.
	 */
	fun getAllInsurances(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement>
}
