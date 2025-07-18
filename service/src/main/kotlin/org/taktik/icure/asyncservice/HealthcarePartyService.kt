/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface HealthcarePartyService {
	suspend fun getHealthcareParty(id: String): HealthcareParty?
	fun listHealthcarePartiesBy(searchString: String, offset: Int, limit: Int): Flow<HealthcareParty>

	@Deprecated(message = "A HCP may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	suspend fun getHcPartyKeysForDelegate(healthcarePartyId: String): Map<String, String>

	suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>>

	suspend fun modifyHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty?

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted HealthcareParties.
	 */
	fun deleteHealthcareParties(ids: List<IdAndRev>): Flow<HealthcareParty>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the latest rev of the entity to delete.
	 * @return the deleted [HealthcareParty].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteHealthcareParty(id: String, rev: String?): HealthcareParty

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
	suspend fun purgeHealthcareParty(id: String, rev: String): DocIdentifier

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity
	 * @param id the id of the entity marked to restore
	 * @param rev the revision of the entity after it was marked as deleted
	 * @return the restored entity
	 */
	suspend fun undeleteHealthcareParty(id: String, rev: String): HealthcareParty

	suspend fun createHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty?

	/**
	 * Retrieves all the healthcare parties in a group, sorted by [HealthcareParty.lastName], in a format for pagination.
	 * This method will redact the sensitive information from the hcp if the current user does not have the permission
	 * to read them.
	 *
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by [HealthcareParty.lastName] in descending or ascending order.
	 * @return a [Flow] of [PaginationElement] wrapping the [HealthcareParty] entities.
	 * @throws AccessDeniedException if the current user does not meet the precondition to find [HealthcareParty] entities.
	 */
	fun findHealthcarePartiesBy(offset: PaginationOffset<String>, desc: Boolean?): Flow<PaginationElement>

	/**
	 * Retrieves all the healthcare parties in a group, sorted by the concatenation of [HealthcareParty.lastName] and
	 * [HealthcareParty.firstName] normalized removing all the characters that are not letters and mapping all the characters
	 * outside the standard english alphabet to letters of the alphabet.
	 * The result will be provided in a format for pagination and will redact the sensitive information from the hcp if
	 * the current user does not have the permission to read them.
	 *
	 * @param fuzzyName a prefix that will match the normalized [HealthcareParty.lastName] and [HealthcareParty.firstName] concatenation.
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by [HealthcareParty.lastName] in descending or ascending order.
	 * @return a [Flow] of [PaginationElement] wrapping the [HealthcareParty] entities.
	 * @throws AccessDeniedException if the current user does not meet the precondition to find [HealthcareParty] entities.
	 */
	fun findHealthcarePartiesBy(fuzzyName: String, offset: PaginationOffset<String>, desc: Boolean?): Flow<PaginationElement>
	fun listHealthcarePartiesByNihii(nihii: String): Flow<HealthcareParty>
	fun listHealthcarePartiesBySsin(ssin: String): Flow<HealthcareParty>
	fun listHealthcarePartiesByName(name: String): Flow<HealthcareParty>

	suspend fun getPublicKey(healthcarePartyId: String): String?

	/**
	 * Retrieves all the [HealthcareParty] entities in a group where the [HealthcareParty.speciality] is equal to [type],
	 * [HealthcareParty.nihiiSpecCode] is equal to [spec], and they have a postal code in [HealthcareParty.addresses] that
	 * is between [firstCode] and [lastCode].
	 * The results will be returned in a format for pagination but only the users that have the permissions to read the
	 * redacted information of a doctor can access this method.
	 *
	 * @param type the [HealthcareParty.speciality].
	 * @param spec the [HealthcareParty.nihiiSpecCode].
	 * @param firstCode the upper bound for the postal codes of the healthcare parties.
	 * @param lastCode the lower bound for the postal codes of the healthcare parties.
	 * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [HealthcareParty] entities.
	 * @throws AccessDeniedException if the current user cannot read the redacted information of a doctor.
	 */
	fun listHealthcarePartiesBySpecialityAndPostcode(type: String, spec: String, firstCode: String, lastCode: String, offset: PaginationOffset<ComplexKey>): Flow<PaginationElement>
	fun getHealthcareParties(ids: List<String>): Flow<HealthcareParty>

	/**
	 * Retrieves all the [HealthcareParty] entities which [HealthcareParty.ssin], [HealthcareParty.nihii],
	 * [HealthcareParty.cbe], or [HealthcareParty.ehp] matches the [searchValue] passed as parameter in a format for pagination.
	 * Only users that have the permissions to read the redacted information of a doctor can access this method.
	 *
	 * @param searchValue a query to search against the fields.
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by descending or ascending order.
	 * @return a [Flow] of [PaginationElement] wrapping the [HealthcareParty] entities.
	 * @throws AccessDeniedException if the current user cannot read the redacted information of a doctor.
	 */
	fun findHealthcarePartiesBySsinOrNihii(searchValue: String, paginationOffset: PaginationOffset<String>, desc: Boolean): Flow<PaginationElement>
	fun getHealthcarePartiesByParentId(parentId: String): Flow<HealthcareParty>
	suspend fun getHcpHierarchyIds(sender: HealthcareParty): HashSet<String>
	fun filterHealthcareParties(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<HealthcareParty>): Flow<ViewQueryResultEvent>
	fun modifyHealthcareParties(entities: Collection<HealthcareParty>): Flow<HealthcareParty>

	/**
	 * Retrieves the ids of the [HealthcareParty]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [HealthcareParty].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the current user does not have the Permission to search codes with a filter.
	 */
	fun matchHealthcarePartiesBy(filter: AbstractFilter<HealthcareParty>): Flow<String>
}
