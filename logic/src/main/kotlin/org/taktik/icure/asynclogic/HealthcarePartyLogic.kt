/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.pagination.PaginationElement

interface HealthcarePartyLogic : EntityPersister<HealthcareParty, String> {

	suspend fun getHealthcareParty(id: String): HealthcareParty?
	fun listHealthcarePartiesBy(searchString: String, offset: Int, limit: Int): Flow<HealthcareParty>

	@Deprecated(message = "A HCP may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	suspend fun getHcPartyKeysForDelegate(healthcarePartyId: String): Map<String, String>

	suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>>

	suspend fun modifyHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty?
	fun deleteHealthcareParties(healthcarePartyIds: List<String>): Flow<DocIdentifier>

	suspend fun createHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty?

	/**
	 * Retrieves all the healthcare parties in a group, sorted by [HealthcareParty.lastName], in a format for pagination.
	 *
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by [HealthcareParty.lastName] in descending or ascending order.
	 * @return a [Flow] of [PaginationElement] wrapping the [HealthcareParty] entities.
	 */
	fun findHealthcarePartiesBy(offset: PaginationOffset<String>, desc: Boolean?): Flow<PaginationElement>

	/**
	 * Retrieves all the healthcare parties in a group, sorted by the concatenation of [HealthcareParty.lastName] and
	 * [HealthcareParty.firstName] normalized removing all the characters that are not letters and mapping all the characters
	 * outside the standard english alphabet to letters of the alphabet.
	 * If a [fuzzyName] is passed, only the healthcare party which normalized key starts with the normalized [fuzzyName]
	 * will be returned.
	 * The result will be provided in a format for pagination.
	 *
	 * @param fuzzyName a prefix that will match the normalized [HealthcareParty.lastName] and [HealthcareParty.firstName] concatenation.
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by [HealthcareParty.lastName] in descending or ascending order.
	 * @return a [Flow] of [PaginationElement] wrapping the [HealthcareParty] entities.
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
	 * The results will be returned in a format for pagination.
	 *
	 * @param type the [HealthcareParty.speciality].
	 * @param spec the [HealthcareParty.nihiiSpecCode].
	 * @param firstCode the upper bound for the postal codes of the healthcare parties.
	 * @param lastCode the lower bound for the postal codes of the healthcare parties.
	 * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [HealthcareParty] entities.
	 */
	fun listHealthcarePartiesBySpecialityAndPostcode(type: String, spec: String, firstCode: String, lastCode: String, offset: PaginationOffset<ComplexKey>): Flow<PaginationElement>
	fun getHealthcareParties(ids: List<String>): Flow<HealthcareParty>

	/**
	 * Retrieves all the [HealthcareParty] entities which [HealthcareParty.ssin], [HealthcareParty.nihii],
	 * [HealthcareParty.cbe], or [HealthcareParty.ehp] matches the [searchValue] passed as parameter in a format for
	 * pagination.
	 *
	 * @param searchValue a query to search against the defined fields.
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by descending or ascending order.
	 * @return a [Flow] of [PaginationElement] wrapping the [HealthcareParty] entities.
	 */
	fun findHealthcarePartiesBySsinOrNihii(searchValue: String, paginationOffset: PaginationOffset<String>, desc: Boolean): Flow<PaginationElement>
	fun getHealthcarePartiesByParentId(parentId: String): Flow<HealthcareParty>
	suspend fun getHcpHierarchyIds(sender: HealthcareParty): HashSet<String>
	fun filterHealthcareParties(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<HealthcareParty>): Flow<ViewQueryResultEvent>
}
