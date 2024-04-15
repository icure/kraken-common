/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.exceptions.NotFoundRequestException

interface HealthElementService : EntityWithSecureDelegationsService<HealthElement> {
	suspend fun createHealthElement(healthElement: HealthElement): HealthElement?

	suspend fun getHealthElement(healthElementId: String): HealthElement?
	fun getHealthElements(healthElementIds: Collection<String>): Flow<HealthElement>

	/**
	 * Retrieves all the [HealthElement]s for a given healthcare party id and secret patient key in a format for
	 * pagination.
	 * This method will filter out all the [HealthElement]s that the current user is not allowed to access.
	 * Note: if the current user data owner id is equal to [hcPartyId], then all the search keys available for the
	 * current user will be considered when retrieving the [HealthElement]s.
	 *
	 * @param hcPartyId the id of the healthcare party to look for in the delegations.
	 * @param secretPatientKeys the secret patient keys, that will be searched in [HealthElement.secretForeignKeys].
	 * @return a [Flow] of [HealthElement]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to list [HealthElement]s.
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
	 * @throws AccessDeniedException if [dataOwnerId] is not the current data owner id and is not among the access keys
	 * and the current user does not have the permission to search Calendar Items for other users.
	 */
	fun listHealthElementIdsByDataOwnerPatientOpeningDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
	fun listHealthElementIdsByHcPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<String>

	fun listHealthElementIdsByHcParty(hcpId: String): Flow<String>
	suspend fun listLatestHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): List<HealthElement>
	fun listHealthElementIdsByHcPartyAndCodes(hcPartyId: String, codeType: String, codeNumber: String): Flow<String>
	fun listHealthElementIdsByHcPartyAndTags(hcPartyId: String, tagType: String, tagCode: String): Flow<String>
	fun listHealthElementsIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>): Flow<String>
	fun listHealthElementIdsByHcPartyAndStatus(hcPartyId: String, status: Int): Flow<String>

	/**
	 * Deletes a batch of [HealthElement]s.
	 * If the user does not have the permission to delete an [HealthElement] or the [HealthElement] does not exist, then it
	 * is ignored without any warning.
	 *
	 * @param ids a [List] containing the ids of the [HealthElement]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [HealthElement]s successfully deleted.
	 */
	fun deleteHealthElements(ids: Set<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [HealthElement].
	 *
	 * @param id the id of the [HealthElement] to delete.
	 * @return a [DocIdentifier] related to the [HealthElement] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [HealthElement].
	 * @throws [NotFoundRequestException] if an [HealthElement] with the specified [id] does not exist.
	 */
	suspend fun deleteHealthElement(id: String): DocIdentifier

	suspend fun modifyHealthElement(healthElement: HealthElement): HealthElement?

	suspend fun addDelegations(healthElementId: String, delegations: List<Delegation>): HealthElement?

	fun solveConflicts(limit: Int? = null): Flow<IdAndRev>
	fun filter(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<HealthElement>
	): Flow<ViewQueryResultEvent>
	fun modifyEntities(entities: Flow<HealthElement>): Flow<HealthElement>
	fun createEntities(entities: Flow<HealthElement>): Flow<HealthElement>
}

fun HealthElementService.modifyEntities(entities: Collection<HealthElement>): Flow<HealthElement> = this.modifyEntities(entities.asFlow())
fun HealthElementService.createEntities(entities: Collection<HealthElement>): Flow<HealthElement> = this.createEntities(entities.asFlow())
