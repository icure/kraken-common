/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.base.EntityWithConflictResolutionService
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.exceptions.NotFoundRequestException

interface FormService : EntityWithSecureDelegationsService<Form>, EntityWithConflictResolutionService {
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
	 * Retrieves the ids of all the [Form]s given the [dataOwnerId] (plus all the current access keys if that is
	 * equal to the data owner id of the user making the request) and a set of [Form.secretForeignKeys].
	 * Only the ids of the Forms where [Form.openingDate] is not null are returned and the results are sorted by
	 * [Form.openingDate] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param dataOwnerId the id of a data owner.
	 * @param secretForeignKeys a [Set] of [Form.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the Forms where [Form.openingDate] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the Forms where [Form.openingDate] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [Form.openingDate] ascending or descending.
	 * @return a [Flow] of Form ids.
	 * @throws AccessDeniedException if [dataOwnerId] is not the current data owner id and is not among the access keys
	 * and the current user does not have the permission to search Forms for other users.
	 */
	fun listFormIdsByDataOwnerPatientOpeningDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

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
	override fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev>

	/**
	 * Returns all the [Form]s where [Form.logicalUuid] is equal to [formUuid], sorted by [Form.created] in ascending or
	 * descending order according to the [descending] parameter.
	 *
	 * @param formUuid the [Form.logicalUuid].
	 * @param descending whether to sort the result in descending or ascending order by [Form.created].
	 * @return a [Flow] of [Form]s.
	 */
	fun listFormsByLogicalUuid(formUuid: String, descending: Boolean): Flow<Form>

	/**
	 * Returns all the [Form]s where [Form.uniqueId] is equal to [lid], sorted by [Form.created] in ascending or
	 * descending order according to the [descending] parameter.
	 *
	 * @param lid the [Form.uniqueId].
	 * @param descending whether to sort the result in descending or ascending order by [Form.created].
	 * @return a [Flow] of [Form]s.
	 */
	fun listFormsByUniqueId(lid: String, descending: Boolean): Flow<Form>

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

	/**
	 * Retrieves the ids of the [Form]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [Form].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchFormsBy(filter: AbstractFilter<Form>): Flow<String>
}
