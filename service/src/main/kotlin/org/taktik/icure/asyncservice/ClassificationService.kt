/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Classification
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException

interface ClassificationService : EntityWithSecureDelegationsService<Classification> {
	suspend fun createClassification(classification: Classification): Classification?

	suspend fun getClassification(classificationId: String): Classification?

	/**
	 * Retrieves all the [Classification]s for a healthcare party and a list of [Classification.secretForeignKeys].
	 * Note: if the current user data owner id is equal to [hcPartyId], then also all the available search keys for the
	 * user will be considered in retrieving the results.
	 * This method will filter out all the entities that the current user is not allowed to access.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKeys the patient secret foreign keys.
	 * @return a [Flow] of [Classification]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to list [Classification]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<Classification>

	/**
	 * Retrieves the ids of all the [Classification]s given the [dataOwnerId] (and its access keys if it is the current
	 * user making the request) and a set of [Classification.secretForeignKeys].
	 * Only the ids of the Classifications where [Classification.created] is not null are returned and the results are sorted by
	 * [Classification.created] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param dataOwnerId the id of the data owner.
	 * @param secretForeignKeys a [Set] of [Classification.secretForeignKeys].
	 * @param startDate a timestamp. If not null, only the ids of the Classifications where [Classification.created] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a timestamp. If not null, only the ids of the Classifications where [Classification.created] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [Classification.created] ascending or descending.
	 * @return a [Flow] of Classification ids.
	 * @throws AccessDeniedException if [dataOwnerId] is not the current data owner id and is not among the access keys
	 * and the current user does not have the permission to search Classifications for other users.
	 */
	fun listClassificationIdsByDataOwnerPatientCreated(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

    /**
     * Marks a batch of entities as deleted.
     * The data of the entities is preserved, but they won't appear in most queries.
     * Ignores entities that:
     * - don't exist
     * - the user can't delete due to limited lack of write access
     * - don't match the provided revision (if provided)
     *
     * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
     * @return a [Flow] containing the [DocIdentifier]s of the entities successfully deleted.
     */
    fun deleteClassifications(ids: List<IdAndRev>): Flow<DocIdentifier>

    /**
     * Marks an entity as deleted.
     * The data of the entity is preserved, but the entity won't appear in most queries.
     *
     * @param id the id of the entity to delete.
     * @param rev
     * @return the updated [DocIdentifier] for the entity.
     * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
     * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
     * @throws ConflictRequestException if the entity rev doesn't match.
     */
    suspend fun deleteClassification(id: String, rev: String?): DocIdentifier

    /**
     * Deletes an entity.
     * An entity deleted this way can't be restored.
     * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
     *
     * @param id the id of the entity
     * @param rev the latest known revision of the entity.
     * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
     * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
     * @throws ConflictRequestException if the entity rev doesn't match.
     */
    suspend fun purgeClassification(id: String, rev: String): DocIdentifier

    /**
     * Restores an entity marked as deleted.
     * The user needs to have write access to the entity
     * @param id the id of the entity marked to restore
     * @param rev the revision of the entity after it was marked as deleted
     * @return the restored entity
     */
    suspend fun undeleteClassification(id: String, rev: String): Classification
	suspend fun modifyClassification(classification: Classification): Classification?

	suspend fun addDelegation(classificationId: String, healthcarePartyId: String, delegation: Delegation): Classification?

	suspend fun addDelegations(classificationId: String, delegations: List<Delegation>): Classification?

	/**
	 * Retrieves a batch of [Classification]s by their ids.
	 * This method will automatically filter out all the [Classification]s that the current user cannot access.
	 *
	 * @param ids the ids of the Classifications to retrieve.
	 * @return a [Flow] of [Classification]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to retrieve [Classification]s.
	 */
	fun getClassifications(ids: List<String>): Flow<Classification>

	/**
	 * Updates a collection of [Classification]s.
	 *
	 * This method will automatically filter out all the changes that the user is not authorized to make, either because
	 * they are not valid or because the user does not have the correct permission to apply them. No error will be
	 * returned for the filtered out entities.
	 *
	 * @param entities a [Collection] of updated [Classification].
	 * @return a [Flow] containing all the [Classification]s successfully updated.
	 */
	fun modifyEntities(entities: Collection<Classification>): Flow<Classification>

	/**
	 * Retrieves the ids of the [Classification]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [Classification].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchClassificationsBy(filter: AbstractFilter<Classification>): Flow<String>
}
