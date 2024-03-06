/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface ClassificationTemplateService : EntityWithSecureDelegationsService<ClassificationTemplate> {
	suspend fun createClassificationTemplate(classificationTemplate: ClassificationTemplate): ClassificationTemplate?

	suspend fun getClassificationTemplate(classificationTemplateId: String): ClassificationTemplate?

	/**
	 * Deletes [ClassificationTemplate]s in batch.
	 * If the user does not meet the precondition to delete [ClassificationTemplate]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [Collection] containing the ids of the [ClassificationTemplate]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [ClassificationTemplate]s that were successfully deleted.
	 */
	fun deleteClassificationTemplates(ids: Collection<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [ClassificationTemplate].
	 *
	 * @param classificationTemplateId the id of the [ClassificationTemplate] to delete.
	 * @return a [DocIdentifier] related to the [ClassificationTemplate] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [ClassificationTemplate].
	 * @throws [NotFoundRequestException] if an [ClassificationTemplate] with the specified [classificationTemplateId] does not exist.
	 */
	suspend fun deleteClassificationTemplate(classificationTemplateId: String): DocIdentifier

	suspend fun modifyClassificationTemplate(classificationTemplate: ClassificationTemplate): ClassificationTemplate

	suspend fun addDelegation(classificationTemplateId: String, healthcarePartyId: String, delegation: Delegation): ClassificationTemplate?

	suspend fun addDelegations(classificationTemplateId: String, delegations: List<Delegation>): ClassificationTemplate?
	fun getClassificationTemplates(ids: List<String>): Flow<ClassificationTemplate>

	/**
	 * Retrieves all the [ClassificationTemplate]s for a healthcare party and a list of [ClassificationTemplate.secretForeignKeys].
	 * Note: if the current user data owner id is equal to [hcPartyId], then also all the available search keys for the
	 * user will be considered in retrieving the results.
	 * This method will filter out all the entities that the current user is not allowed to access.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKeys the patient secret foreign keys.
	 * @return a [Flow] of [ClassificationTemplate]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to list [ClassificationTemplate]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<ClassificationTemplate>

	/**
	 * Retrieves all the [ClassificationTemplate]s for a healthcare party and a list of [ClassificationTemplate.secretForeignKeys],
	 * and a single [ClassificationTemplate.secretForeignKeys] in a format for pagination.
	 * Note: differently from [listClassificationsByHCPartyAndSecretPatientKeys] this method will NOT consider the
	 * available search keys for the current user, even if their data owner id is equal to [hcPartyId].
	 * This method will filter out all the entities that the current user is not allowed to access, but it will guarantee
	 * that the page size specified in the [paginationOffset] is reached as long as there are available elements.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKey the patient secret foreign key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [ClassificationTemplate]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to list [ClassificationTemplate]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKey(hcPartyId: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	/**
	 * Retrieves all the [ClassificationTemplate]s in a group in a format for pagination.
	 * This method will filter out all the entities that the current user is not allowed to access, but it will guarantee
	 * that the page size specified in the [paginationOffset] is reached as long as there are available elements.
	 *
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [ClassificationTemplate]s.
	 */
	fun listClassificationTemplates(paginationOffset: PaginationOffset<String>): Flow<PaginationElement>
}
