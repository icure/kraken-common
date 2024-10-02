/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.embed.Delegation

interface FormLogic : EntityPersister<Form>, EntityWithSecureDelegationsLogic<Form> {
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
	@Deprecated("This method cannot include results with secure delegations, use listFormIdsByDataOwnerPatientOpeningDate instead")
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
	 */
	fun listFormIdsByDataOwnerPatientOpeningDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
	suspend fun addDelegation(formId: String, delegation: Delegation): Form?

	suspend fun createForm(form: Form): Form?

	suspend fun modifyForm(form: Form): Form?
	fun listByHcPartyAndParentId(hcPartyId: String, formId: String): Flow<Form>

	suspend fun addDelegations(formId: String, delegations: List<Delegation>): Form?
	fun solveConflicts(limit: Int? = null, ids: List<String>? = null): Flow<IdAndRev>

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
}
