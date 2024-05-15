/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.entities.Classification
import org.taktik.icure.entities.embed.Delegation
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
	 * Deletes [Classification]s in batch.
	 * If the user does not meet the precondition to delete [Classification]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [Set] containing the ids of the [Classification]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [Classification]s that were successfully deleted.
	 */
	fun deleteClassifications(ids: Set<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [Classification].
	 *
	 * @param classificationId the id of the [Classification] to delete.
	 * @return a [DocIdentifier] related to the [Classification] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [Classification].
	 * @throws [NotFoundRequestException] if an [Classification] with the specified [classificationId] does not exist.
	 */
	suspend fun deleteClassification(classificationId: String): DocIdentifier
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
}
