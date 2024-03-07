/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.embed.Delegation
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface FormService : EntityWithSecureDelegationsService<Form> {
	suspend fun getForm(id: String): Form?
	fun getForms(selectedIds: Collection<String>): Flow<Form>

	/**
	 * Retrieves all the [Form]s for the provided healthcare party id and secret patient keys, filtering the results by
	 * [healthElementId], [planOfActionId], or [formTemplateId] if specified. If more than one of these parameters is
	 * not null, only the [Form]s that match all the conditions will be returned.
	 * This method will filter out all the entities that the current user is not allowed to access.
	 * Note: if the current user data owner id is equal to [hcPartyId], then all the available search keys for the current
	 * user will be considered when retrieving the [Form]s.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKeys a [List] of secret patient keys.
	 * @param healthElementId if not null, only the [Form]s with this [Form.healthElementId] will be retrieved.
	 * @param planOfActionId if not null, only the [Form]s with this [Form.planOfActionId] will be retrieved.
	 * @param formTemplateId if not null, only the [Form]s with this [Form.formTemplateId] will be retrieved.
	 * @return a [Flow] of [Form]s.
	 * @throws AccessDeniedException if the current user does not match the precondition to list [Form]s.
	 */
	fun listFormsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>, healthElementId: String?, planOfActionId: String?, formTemplateId: String?): Flow<Form>

	/**
	 * Retrieves all the [Form]s for the provided healthcare party id and secret patient key in a format for pagination.
	 * This method will filter out all the entities that the current user cannot access, but it will ensure tha the page
	 * will be filled as long as there are available elements.
	 * Note: differently from [listFormsByHCPartyAndPatient], this method will NOT take into account the available search
	 * keys for the current user.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKey the secret patient key.
	 * @return a [Flow] of [PaginationElement]s wrapping the [Form]s.
	 */
	fun listFormsByHcPartyIdPatientSecretKey(hcPartyId: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	suspend fun addDelegation(formId: String, delegation: Delegation): Form?

	suspend fun createForm(form: Form): Form?

	/**
	 * Deletes a batch of [Form]s.
	 * If the user does not have the permission to delete an [Form] or the [Form] does not exist, then it
	 * is ignored without any warning.
	 *
	 * @param ids a [List] containing the ids of the [Form]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [Form]s successfully deleted.
	 */
	fun deleteForms(ids: Set<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [Form].
	 *
	 * @param id the id of the [Form] to delete.
	 * @return a [DocIdentifier] related to the [Form] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [Form].
	 * @throws [NotFoundRequestException] if an [Form] with the specified [id] does not exist.
	 */
	suspend fun deleteForm(id: String): DocIdentifier

	suspend fun modifyForm(form: Form): Form?
	fun listByHcPartyAndParentId(hcPartyId: String, formId: String): Flow<Form>

	suspend fun addDelegations(formId: String, delegations: List<Delegation>): Form?
	fun solveConflicts(limit: Int? = null): Flow<IdAndRev>
	suspend fun getAllByLogicalUuid(formUuid: String): List<Form>
	suspend fun getAllByUniqueId(lid: String): List<Form>

	/**
	 * Updates a batch of [Form]s. If any of the [Form]s in the batch specifies an invalid modification, then will be
	 * not applied but no error will be thrown.
	 * @param forms a [Collection] of updated [Form]s
	 * @return a [Flow] containing the successfully updated [Form]s.
	 */
	fun modifyForms(forms: Collection<Form>): Flow<Form>

	/**
	 * Creates [Form]s in batch. The user can perform this operation if they have the permission to create a single form.
	 * @param forms a [Collection] of [Form]s to create.
	 * @return a [Flow] containing the successfully created [Form]s
	 */
	fun createForms(forms: Collection<Form>): Flow<Form>
}
