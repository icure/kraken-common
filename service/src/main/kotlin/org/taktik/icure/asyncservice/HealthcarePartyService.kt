/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginatedElement

interface HealthcarePartyService {
	suspend fun getHealthcareParty(id: String): HealthcareParty?
	fun listHealthcarePartiesBy(searchString: String, offset: Int, limit: Int): Flow<HealthcareParty>

	@Deprecated(message = "A HCP may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	suspend fun getHcPartyKeysForDelegate(healthcarePartyId: String): Map<String, String>

	suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>>

	suspend fun modifyHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty?

	/**
	 * Deletes a batch of [HealthcareParty]s.
	 * If the user does not have the permission to delete an [HealthcareParty] or the [HealthcareParty] does not exist, then it
	 * is ignored without any warning.
	 *
	 * @param healthcarePartyIds a [List] containing the ids of the [HealthcareParty]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [HealthcareParty]s successfully deleted.
	 */
	fun deleteHealthcareParties(healthcarePartyIds: List<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [HealthcareParty].
	 *
	 * @param healthcarePartyId the id of the [HealthcareParty] to delete.
	 * @return a [DocIdentifier] related to the [HealthcareParty] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [HealthcareParty]
	 * in the specified group.
	 * @throws [NotFoundRequestException] if an [HealthcareParty] with the specified [healthcarePartyId] does not exist.
	 */
	suspend fun deleteHealthcareParty(healthcarePartyId: String): DocIdentifier

	suspend fun createHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty?

	/**
	 * Retrieves all the healthcare parties in a group, sorted by [HealthcareParty.lastName], in a format for pagination.
	 * This method will redact the sensitive information from the hcp if the current user does not have the permission
	 * to read them.
	 *
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by [HealthcareParty.lastName] in descending or ascending order.
	 * @return a [Flow] of [PaginatedElement] wrapping the [HealthcareParty] entities.
	 * @throws AccessDeniedException if the current user does not meet the precondition to find [HealthcareParty] entities.
	 */
	fun findHealthcarePartiesBy(offset: PaginationOffset<String>, desc: Boolean?): Flow<PaginatedElement>

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
	 * @return a [Flow] of [PaginatedElement] wrapping the [HealthcareParty] entities.
	 * @throws AccessDeniedException if the current user does not meet the precondition to find [HealthcareParty] entities.
	 */
	fun findHealthcarePartiesBy(fuzzyName: String, offset: PaginationOffset<String>, desc: Boolean?): Flow<PaginatedElement>
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
	 * @return a [Flow] of [PaginatedElement] wrapping the [HealthcareParty] entities.
	 * @throws AccessDeniedException if the current user cannot read the redacted information of a doctor.
	 */
	fun listHealthcarePartiesBySpecialityAndPostcode(type: String, spec: String, firstCode: String, lastCode: String, offset: PaginationOffset<ComplexKey>): Flow<PaginatedElement>
	fun getHealthcareParties(ids: List<String>): Flow<HealthcareParty>

	/**
	 * Retrieves all the [HealthcareParty] entities which [HealthcareParty.ssin], [HealthcareParty.nihii],
	 * [HealthcareParty.cbe], or [HealthcareParty.ehp] matches the [searchValue] passed as parameter in a format for pagination.
	 * Only users that have the permissions to read the redacted information of a doctor can access this method.
	 *
	 * @param searchValue a query to search against the fields.
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by descending or ascending order.
	 * @return a [Flow] of [PaginatedElement] wrapping the [HealthcareParty] entities.
	 * @throws AccessDeniedException if the current user cannot read the redacted information of a doctor.
	 */
	fun findHealthcarePartiesBySsinOrNihii(searchValue: String, paginationOffset: PaginationOffset<String>, desc: Boolean): Flow<PaginatedElement>
	fun getHealthcarePartiesByParentId(parentId: String): Flow<HealthcareParty>
	suspend fun getHcpHierarchyIds(sender: HealthcareParty): HashSet<String>
	fun filterHealthcareParties(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<HealthcareParty>): Flow<ViewQueryResultEvent>
	fun listHealthcarePartyIdsByIdentifiers(hcpIdentifiers: List<Identifier>): Flow<String>
	fun listHealthcarePartyIdsByCode(codeType: String, codeCode: String?): Flow<String>
	fun listHealthcarePartyIdsByTag(tagType: String, tagCode: String?): Flow<String>
	fun modifyHealthcareParties(entities: Collection<HealthcareParty>): Flow<HealthcareParty>
}
