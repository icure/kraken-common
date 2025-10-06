/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.entities.Classification
import org.taktik.icure.entities.embed.Delegation

interface ClassificationLogic :
	EntityPersister<Classification>,
	EntityWithSecureDelegationsLogic<Classification> {

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
	@Deprecated("This method is inefficient for high volumes of keys, use listClassificationIdsByDataOwnerPatientCreated instead")
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
	 */
	fun listClassificationIdsByDataOwnerPatientCreated(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	suspend fun addDelegation(classification: Classification, healthcarePartyId: String, delegation: Delegation): Classification?
	suspend fun addDelegations(classification: Classification, delegations: List<Delegation>): Classification?

	/**
	 * Retrieves a batch of [Classification]s by their ids.
	 *
	 * @param ids the ids of the Classifications to retrieve.
	 * @return a [Flow] of [Classification]s.
	 */
	fun getClassifications(ids: List<String>): Flow<Classification>
}
