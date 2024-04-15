/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier

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
	 * Retrieves the ids of all the [HealthElement]s given the [dataOwnerId] (plus all the current access keys if that is
	 * equal to the data owner id of the user making the request) and a set of [HealthElement.secretForeignKeys].
	 * Only the ids of the HealthElements where [HealthElement.openingDate] is not null are returned and the results are sorted by
	 * [HealthElement.openingDate] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param dataOwnerId the id of the data owner.
	 * @param secretForeignKeys a [Set] of [HealthElement.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the HealthElements where [HealthElement.openingDate] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the HealthElements where [HealthElement.openingDate] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [HealthElement.openingDate] ascending or descending.
	 * @return a [Flow] of HealthElement ids.
	 */
	fun listHealthElementIdsByDataOwnerPatientOpeningDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

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
