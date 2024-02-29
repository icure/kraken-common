/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Classification
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.pagination.PaginationElement

interface ClassificationLogic : EntityPersister<Classification, String>, EntityWithSecureDelegationsLogic<Classification> {

	suspend fun createClassification(classification: Classification): Classification?

	suspend fun getClassification(classificationId: String): Classification?

	/**
	 * Retrieves all the [Classification]s for a healthcare party and a list of [Classification.secretForeignKeys].
	 * Note: if the current user data owner id is equal to [hcPartyId], then also all the available search keys for the
	 * user will be considered in retrieving the results.
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKeys the patient secret foreign keys.
	 * @return a [Flow] of [Classification]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<Classification>

	/**
	 * Retrieves all the [Classification]s for a healthcare party and a list of [Classification.secretForeignKeys],
	 * and a single [Classification.secretForeignKeys] in a format for pagination.
	 * Note: differently from [listClassificationsByHCPartyAndSecretPatientKeys] this method will NOT consider the
	 * available search keys for the current user, even if their data owner id is equal to [hcPartyId].
	 *
	 * @param hcPartyId the healthcare party id.
	 * @param secretPatientKey the patient secret foreign key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [Classification]s.
	 */
	fun listClassificationsByHCPartyAndSecretPatientKey(hcPartyId: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	fun deleteClassifications(ids: Collection<String>): Flow<DocIdentifier>
	fun deleteClassifications(ids: Flow<String>): Flow<DocIdentifier>

	suspend fun addDelegation(classification: Classification, healthcarePartyId: String, delegation: Delegation): Classification?
	suspend fun addDelegations(classification: Classification, delegations: List<Delegation>): Classification?
	fun getClassifications(ids: List<String>): Flow<Classification>
}
