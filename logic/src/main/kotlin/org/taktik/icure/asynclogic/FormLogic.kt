/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.embed.Delegation
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginationElement

interface FormLogic : EntityPersister<Form, String>, EntityWithSecureDelegationsLogic<Form> {
	suspend fun getForm(id: String): Form?
	fun getForms(selectedIds: Collection<String>): Flow<Form>

	/**
	 * Retrieves all the [Form]s for the provided healthcare party id and secret patient keys, filtering the results by
	 * [healthElementId], [planOfActionId], or [formTemplateId] if specified. If more than one of these parameters is
	 * not null, only the [Form]s that match all the conditions will be returned.
	 * Note: if the current user data owner id is equal to [hcPartyId], then all the available search keys for the current
	 * user will be considered when retrieving the [Form]s.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKeys a [List] of secret patient keys.
	 * @param healthElementId if not null, only the [Form]s with this [Form.healthElementId] will be retrieved.
	 * @param planOfActionId if not null, only the [Form]s with this [Form.planOfActionId] will be retrieved.
	 * @param formTemplateId if not null, only the [Form]s with this [Form.formTemplateId] will be retrieved.
	 * @return a [Flow] of [Form]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to list [Form]s.
	 */
	fun listFormsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>, healthElementId: String?, planOfActionId: String?, formTemplateId: String?): Flow<Form>

	/**
	 * Retrieves all the [Form]s for the provided healthcare party id and secret patient key in a format for pagination.
	 * Note: differently from [listFormsByHCPartyAndPatient], this method will NOT take into account the available search
	 * keys for the current user.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKey the secret patient key.
	 * @return a [Flow] of [PaginationElement]s wrapping the [Form]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to list [Form]s.
	 */
	fun listFormsByHcPartyIdPatientSecretKey(hcPartyId: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>
	suspend fun addDelegation(formId: String, delegation: Delegation): Form?

	suspend fun createForm(form: Form): Form?
	fun deleteForms(ids: Set<String>): Flow<DocIdentifier>

	suspend fun modifyForm(form: Form): Form?
	fun listByHcPartyAndParentId(hcPartyId: String, formId: String): Flow<Form>

	suspend fun addDelegations(formId: String, delegations: List<Delegation>): Form?
	fun solveConflicts(limit: Int? = null): Flow<IdAndRev>
	suspend fun getAllByLogicalUuid(formUuid: String): List<Form>
	suspend fun getAllByUniqueId(lid: String): List<Form>
}
