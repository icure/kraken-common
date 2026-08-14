/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.embed.Identifier

/**
 * The value of a row of the `by_data_owner_public_keys` view: every public key of the healthcare party the row is
 * for, mapped to the numeric code of the algorithm it must be used with.
 *
 * The codes are resolved by `RsaEncryptionAlgorithm.fromViewCode`; they are not an enum here so that the dao layer
 * deserializes exactly what the view emits, and an unknown code fails where it is interpreted rather than while
 * reading the row.
 */
data class DataOwnerPublicKeysViewValue(
	val pubkeys: Map<String, Int> = emptyMap(),
)

interface HealthcarePartyDAO : ConflictDAO<HealthcareParty> {
	fun listHealthcarePartiesByPublic(datastoreInformation: IDatastoreInformation, public: Boolean): Flow<HealthcareParty>

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
	 * Retrieves all the [HealthcareParty] entities that are direct children of [parentId], either through the legacy
	 * [HealthcareParty.parentId] or through a parent-type link in [HealthcareParty.dataOwnerGroups].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param parentId the id of the parent healthcare party.
	 * @return a [Flow] of [HealthcareParty].
	 */
	fun listHealthcarePartiesByParentId(datastoreInformation: IDatastoreInformation, parentId: String): Flow<HealthcareParty>

	/**
	 * Retrieves all the [HealthcareParty.id]s that are direct children of [parentId], either through the legacy
	 * [HealthcareParty.parentId] or through a parent-type link in [HealthcareParty.dataOwnerGroups].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param parentId the id of the parent healthcare party.
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

	/**
	 * Retrieves the ids of the healthcare parties directly linked to the data owner group with the provided id,
	 * through the legacy [HealthcareParty.parentId] or a [HealthcareParty.dataOwnerGroups] link (a healthcare party
	 * referencing the group both ways is emitted once). The type of these links is not reported here: it is
	 * intrinsic to the group itself, not to each individual link, see
	 * [org.taktik.icure.entities.base.CryptoActor.effectiveGroupLinkType].
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify the group and CouchDB instance.
	 * @param dataOwnerGroupId the id of the data owner representing the group.
	 * @return a [Flow] of [HealthcareParty.id]s.
	 */
	fun listHealthcarePartiesIdsByDataOwnerGroupId(
		datastoreInformation: IDatastoreInformation,
		dataOwnerGroupId: String
	): Flow<String>

	/**
	 * Retrieves at most [limit] healthcare parties directly linked to any of the data owner groups with the
	 * provided ids, through the legacy [HealthcareParty.parentId] or a [HealthcareParty.dataOwnerGroups] link.
	 * The results are not deduplicated: a healthcare party linked to several of [dataOwnerGroupIds] is returned
	 * once per group it is linked to (but only once for a group it references both ways).
	 *
	 * The rows are [org.taktik.couchdb.ViewRowNoDoc] with the id of a group as key, the id of the linked
	 * healthcare party as id, and that healthcare party's own [HealthcareParty.groupLinkType] as value (null if
	 * it relies on the default for its data owner type).
	 *
	 * The groups are visited in the order of [dataOwnerGroupIds]. [startDocumentId] applies only to the first
	 * entry of [dataOwnerGroupIds], since it is the only group that may have been partially returned by a
	 * previous page.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify the group and CouchDB instance.
	 * @param dataOwnerGroupIds the ids of the data owners representing the groups, without duplicates.
	 * @param startDocumentId the id of the first healthcare party to return, for the first of [dataOwnerGroupIds].
	 * @param limit the maximum number of rows to return.
	 * @return a [Flow] of [ViewQueryResultEvent].
	 * @throws IllegalArgumentException if [dataOwnerGroupIds] is empty or has duplicates, or if [limit] is not positive.
	 */
	fun findDataOwnersLinkedToGroups(
		datastoreInformation: IDatastoreInformation,
		dataOwnerGroupIds: List<String>,
		startDocumentId: String?,
		limit: Int
	): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves the public keys of the healthcare parties with the provided ids, with the encryption algorithm
	 * each key must be used with, without loading the healthcare parties themselves.
	 *
	 * The rows are [org.taktik.couchdb.ViewRowNoDoc] with the id of a healthcare party as both key and id, and a
	 * [DataOwnerPublicKeysViewValue] holding all of its keys as value: **one row per healthcare party**, not one
	 * per key. A healthcare party with no key at all, or that doesn't exist, produces no row. The healthcare
	 * parties are visited in the order of [dataOwnerIds].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify the group and CouchDB instance.
	 * @param dataOwnerIds the ids of the healthcare parties.
	 * @return a [Flow] of [ViewQueryResultEvent].
	 */
	fun listHealthcarePartiesPublicKeys(
		datastoreInformation: IDatastoreInformation,
		dataOwnerIds: List<String>
	): Flow<ViewQueryResultEvent>
}
