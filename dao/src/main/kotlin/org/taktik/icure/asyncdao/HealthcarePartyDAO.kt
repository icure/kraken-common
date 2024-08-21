/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.embed.Identifier

interface HealthcarePartyDAO : GenericDAO<HealthcareParty> {
	fun listHealthcarePartiesByNihii(datastoreInformation: IDatastoreInformation, nihii: String?): Flow<HealthcareParty>

	fun listHealthcarePartiesBySsin(datastoreInformation: IDatastoreInformation, ssin: String): Flow<HealthcareParty>

	/**
	 * Retrieves all the [HealthcareParty] entities in a group where the [HealthcareParty.speciality] is equal to [type],
	 * [HealthcareParty.nihiiSpecCode] is equal to [spec], and they have a postal code in [HealthcareParty.addresses] that
	 * is between [firstCode] and [lastCode].
	 * The results will be returned in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param type the [HealthcareParty.speciality].
	 * @param spec the [HealthcareParty.nihiiSpecCode].
	 * @param firstCode the upper bound for the postal codes of the healthcare parties.
	 * @param lastCode the lower bound for the postal codes of the healthcare parties.
	 * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [HealthcareParty] entities.
	 */
	fun listHealthcarePartiesBySpecialityAndPostcode(datastoreInformation: IDatastoreInformation, type: String, spec: String, firstCode: String, lastCode: String, offset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [HealthcareParty.id]s in a group where the [HealthcareParty.speciality] is equal to [type],
	 * [HealthcareParty.nihiiSpecCode] is equal to [spec], and they have a postal code in [HealthcareParty.addresses] that
	 * is between [firstCode] and [lastCode].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param type the [HealthcareParty.speciality].
	 * @param spec the [HealthcareParty.nihiiSpecCode].
	 * @param firstCode the upper bound for the postal codes of the healthcare parties.
	 * @param lastCode the lower bound for the postal codes of the healthcare parties.
	 * @return a [Flow] of [HealthcareParty.id]s.
	 */
	fun listHealthcarePartyIdsBySpecialityAndPostcode(datastoreInformation: IDatastoreInformation, type: String, spec: String, firstCode: String, lastCode: String): Flow<String>

	/**
	 * Retrieves all the [HealthcareParty] entities in a group, sorted by [HealthcareParty.lastName], in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param pagination a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by [HealthcareParty.lastName] in descending or ascending order.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [HealthcareParty] entities.
	 */
	fun findHealthCareParties(datastoreInformation: IDatastoreInformation, pagination: PaginationOffset<String>, desc: Boolean?): Flow<ViewQueryResultEvent>

	fun listHealthcarePartiesByName(datastoreInformation: IDatastoreInformation, name: String): Flow<HealthcareParty>

	/**
	 * Retrieves all the [HealthcareParty] entities where [HealthcareParty.ssin], [HealthcareParty.nihii],
	 * [HealthcareParty.cbe], or [HealthcareParty.ehp] start with the [searchValue] passed as parameter
	 * in a format for pagination.
	 * The results will be sorted lexicographically by these identifiers.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchValue the value to search against the fields. If null, all the entities with a value in those fields will be returned.
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by descending or ascending order.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [HealthcareParty] entities.
	 */
	fun findHealthcarePartiesBySsinOrNihii(datastoreInformation: IDatastoreInformation, searchValue: String?, offset: PaginationOffset<String>, desc: Boolean?): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [HealthcareParty.id]s where [HealthcareParty.ssin], [HealthcareParty.nihii],
	 * [HealthcareParty.cbe], or [HealthcareParty.ehp] start with the [searchValue] passed as parameter.
	 * The results will be sorted lexicographically by these identifiers.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchValue the value to search against the fields. If null, all the entities with a value in those fields will be returned.
	 * @param desc whether return the healthcare parties sorted by descending or ascending order.
	 * @return a [Flow] of [HealthcareParty.id]s.
	 */
	fun listHealthcarePartyIdsBySsinOrNihii(datastoreInformation: IDatastoreInformation, searchValue: String?, desc: Boolean): Flow<String>

	/**
	 * Retrieves all the [HealthcareParty] entities in a group, sorted by the concatenation of [HealthcareParty.lastName] and
	 * [HealthcareParty.firstName] normalized removing all the characters that are not letters and mapping all the characters
	 * outside the standard english alphabet to letters of the alphabet.
	 * If a [searchString] is passed, only the healthcare party which normalized key starts with the normalized [searchString]
	 * will be returned.
	 * The result will be provided in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchString a prefix that will match the normalized [HealthcareParty.lastName] and [HealthcareParty.firstName] concatenation.
	 * @param offset a [PaginationOffset] of [String] for pagination.
	 * @param desc whether return the healthcare parties sorted by [HealthcareParty.lastName] in descending or ascending order.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [HealthcareParty] entities.
	 */
	fun findHealthcarePartiesByHcPartyNameContainsFuzzy(datastoreInformation: IDatastoreInformation, searchString: String?, offset: PaginationOffset<String>, desc: Boolean?): Flow<ViewQueryResultEvent>

	fun listHealthcareParties(datastoreInformation: IDatastoreInformation, searchString: String, offset: Int, limit: Int): Flow<HealthcareParty>

	@Deprecated(message = "A HCP may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	suspend fun getHcPartyKeysForDelegate(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Map<String, String>

	suspend fun getAesExchangeKeysForDelegate(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Map<String, Map<String, Map<String, String>>>

	/**
	 * Retrieves all the [HealthcareParty] entities where [HealthcareParty.parentId] is equal to [parentId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param parentId the [HealthcareParty.parentId].
	 * @return a [Flow] of [HealthcareParty].
	 */
	fun listHealthcarePartiesByParentId(datastoreInformation: IDatastoreInformation, parentId: String): Flow<HealthcareParty>

	/**
	 * Retrieves all the [HealthcareParty.id]s where [HealthcareParty.parentId] is equal to [parentId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param parentId the [HealthcareParty.parentId].
	 * @return a [Flow] of [HealthcareParty.id]s.
	 */
	fun listHealthcarePartyIdsByParentId(datastoreInformation: IDatastoreInformation, parentId: String): Flow<String>

	fun findHealthcarePartiesByIds(datastoreInformation: IDatastoreInformation, hcpIds: Flow<String>): Flow<ViewQueryResultEvent>
	fun listHealthcarePartyIdsByIdentifiers(datastoreInformation: IDatastoreInformation, hcpIdentifiers: List<Identifier>): Flow<String>
	fun listHealthcarePartyIdsByCode(datastoreInformation: IDatastoreInformation, codeType: String, codeCode: String?): Flow<String>
	fun listHealthcarePartyIdsByTag(datastoreInformation: IDatastoreInformation, tagType: String, tagCode: String?): Flow<String>

	/**
	 * Retrieves all the [HealthcareParty.id]s in a group, sorted by the concatenation of [HealthcareParty.lastName] and
	 * [HealthcareParty.firstName] normalized removing all the characters that are not letters and mapping all the characters
	 * outside the standard english alphabet to letters of the alphabet.
	 * Only the [HealthcareParty] where this string starts with the provided [name] will be returned.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param name a prefix that will match the normalized [HealthcareParty.lastName] and [HealthcareParty.firstName] concatenation.
	 * @param desc whether return the healthcare parties sorted by [HealthcareParty.lastName] in descending or ascending order.
	 * @return a [Flow] of [HealthcareParty.id]s.
	 */
	fun listHealthcarePartyIdsByName(datastoreInformation: IDatastoreInformation, name: String, desc: Boolean = false): Flow<String>
}
