/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.pagination.PaginationElement

interface HealthElementLogic : EntityPersister<HealthElement, String>, EntityWithSecureDelegationsLogic<HealthElement> {
	suspend fun getHealthElement(healthElementId: String): HealthElement?
	fun getHealthElements(healthElementIds: Collection<String>): Flow<HealthElement>

	/**
	 * Retrieves all the [HealthElement]s for a given healthcare party id and secret patient key in a format for
	 * pagination.
	 * Note: if the current user data owner id is equal to [hcPartyId], then all the search keys available for the
	 * current user will be considered when retrieving the [HealthElement]s.
	 *
	 * @param hcPartyId the id of the healthcare party to look for in the delegations.
	 * @param secretPatientKeys the secret patient keys, that will be searched in [HealthElement.secretForeignKeys].
	 * @return a [Flow] of [HealthElement]s.
	 */
	fun listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<HealthElement>

	/**
	 * Retrieves all the [HealthElement]s for a given healthcare party id and secret patient key in a format for
	 * pagination.
	 * Note: differently from [listHealthElementsByHcPartyAndSecretPatientKeys], this method will NOT consider the
	 * available search keys for the current user if their data owner id is equal to [hcPartyId].
	 *
	 * @param hcPartyId the id of the healthcare party to look for in the delegations.
	 * @param secretPatientKey the secret patient key, that will be searched in [HealthElement.secretForeignKeys].
	 * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [HealthElement]s.
	 */
	fun listHealthElementsByHCPartyIdAndSecretPatientKey(hcPartyId: String, secretPatientKey: String, offset: PaginationOffset<ComplexKey>): Flow<PaginationElement>
	fun listHealthElementIdsByHcPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<String>

	fun listHealthElementIdsByHcParty(hcpId: String): Flow<String>
	suspend fun listLatestHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): List<HealthElement>
	fun listHealthElementIdsByHcPartyAndCodes(hcPartyId: String, codeType: String, codeNumber: String): Flow<String>
	fun listHealthElementIdsByHcPartyAndTags(hcPartyId: String, tagType: String, tagCode: String): Flow<String>
	fun listHealthElementsIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>): Flow<String>
	fun listHealthElementIdsByHcPartyAndStatus(hcPartyId: String, status: Int): Flow<String>
	fun deleteHealthElements(ids: Set<String>): Flow<DocIdentifier>

	suspend fun modifyHealthElement(healthElement: HealthElement): HealthElement?

	suspend fun addDelegation(healthElementId: String, delegation: Delegation): HealthElement?

	suspend fun addDelegations(healthElementId: String, delegations: List<Delegation>): HealthElement?

	fun solveConflicts(limit: Int? = null): Flow<IdAndRev>

	fun filter(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<HealthElement>
	): Flow<ViewQueryResultEvent>
}
