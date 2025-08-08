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
import org.taktik.icure.asyncservice.base.EntityWithConflictResolutionService
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException

interface HealthElementService :
	EntityWithSecureDelegationsService<HealthElement>,
	EntityWithConflictResolutionService {
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
	@Deprecated("This method is inefficient for high volumes of keys, use listHealthElementIdsByDataOwnerPatientOpeningDate instead")
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
	 * and the current user does not have the permission to search HealthElements for other users.
	 */
	fun listHealthElementIdsByDataOwnerPatientOpeningDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	@Deprecated("This method is inefficient for high volumes of keys, use listHealthElementIdsByDataOwnerPatientOpeningDate instead")
	suspend fun listLatestHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): List<HealthElement>

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [HealthElement]s.
	 */
	fun deleteHealthElements(ids: List<IdAndRev>): Flow<HealthElement>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the latest rev of the entity to delete.
	 * @return the deleted [HealthElement].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteHealthElement(id: String, rev: String?): HealthElement

	/**
	 * Deletes an entity.
	 * An entity deleted this way can't be restored.
	 * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
	 *
	 * @param id the id of the entity
	 * @param rev the latest known revision of the entity.
	 * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun purgeHealthElement(id: String, rev: String): DocIdentifier

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity
	 * @param id the id of the entity marked to restore
	 * @param rev the revision of the entity after it was marked as deleted
	 * @return the restored entity
	 */
	suspend fun undeleteHealthElement(id: String, rev: String): HealthElement

	suspend fun modifyHealthElement(healthElement: HealthElement): HealthElement?

	suspend fun addDelegations(healthElementId: String, delegations: List<Delegation>): HealthElement?

	override fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev>
	fun filter(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<HealthElement>,
	): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves the ids of the [HealthElement]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [HealthElement].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchHealthElementsBy(filter: AbstractFilter<HealthElement>): Flow<String>

	fun modifyEntities(entities: Flow<HealthElement>): Flow<HealthElement>
	fun createEntities(entities: Flow<HealthElement>): Flow<HealthElement>
}

fun HealthElementService.modifyEntities(entities: Collection<HealthElement>): Flow<HealthElement> = this.modifyEntities(entities.asFlow())
fun HealthElementService.createEntities(entities: Collection<HealthElement>): Flow<HealthElement> = this.createEntities(entities.asFlow())
