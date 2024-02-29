/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.pagination.PaginationElement

interface ClassificationTemplateLogic : EntityPersister<ClassificationTemplate, String>, EntityWithSecureDelegationsLogic<ClassificationTemplate> {
	suspend fun createClassificationTemplate(classificationTemplate: ClassificationTemplate): ClassificationTemplate?

	suspend fun getClassificationTemplate(classificationTemplateId: String): ClassificationTemplate?
	fun deleteClassificationTemplates(ids: Set<String>): Flow<DocIdentifier>
	fun deleteClassificationTemplates(ids: Flow<String>): Flow<DocIdentifier>
	suspend fun addDelegation(classificationTemplate: ClassificationTemplate, healthcarePartyId: String, delegation: Delegation): ClassificationTemplate?

	suspend fun addDelegations(classificationTemplate: ClassificationTemplate, delegations: List<Delegation>): ClassificationTemplate?
	fun getClassificationTemplates(ids: Collection<String>): Flow<ClassificationTemplate>

	/**
	 * Retrieves all the [ClassificationTemplate]s for a healthcare party and a list of [ClassificationTemplate.secretForeignKeys].
	 * Note: if the current user data owner id is equal to [hcPartyId], then also all the available search keys for the
	 * user will be considered in retrieving the results.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKeys the patient secret foreign keys.
	 * @return a [Flow] of [ClassificationTemplate]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<ClassificationTemplate>

	/**
	 * Retrieves all the [ClassificationTemplate]s for a healthcare party and a list of [ClassificationTemplate.secretForeignKeys],
	 * and a single [ClassificationTemplate.secretForeignKeys] in a format for pagination.
	 * Note: differently from [listClassificationsByHCPartyAndSecretPatientKeys] this method will NOT consider the
	 * available search keys for the current user, even if their data owner id is equal to [hcPartyId].
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKey the patient secret foreign key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [ClassificationTemplate]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKey(hcPartyId: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	/**
	 * Retrieves all the [ClassificationTemplate]s in a group in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [ClassificationTemplate]s.
	 */
	fun listClassificationTemplates(paginationOffset: PaginationOffset<String>): Flow<PaginationElement>
}
