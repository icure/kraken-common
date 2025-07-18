/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.Identifier

interface PatientDAO : GenericDAO<Patient> {

	/**
	 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.ssin] starts with the
	 * provided [ssin].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param ssin the ssin to search.
	 * @param healthcarePartyId a data owner id or a search key.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyAndSsin(datastoreInformation: IDatastoreInformation, ssin: String, healthcarePartyId: String): Flow<String>

	/**
	 * Retrieves all the [Patient.id]s delegated to a data owner (by checking their [searchKeys]) and where [Patient.active]
	 * is [active].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param active whether to get the active or inactive users.
	 * @param searchKeys the data owner id + access control keys.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByActive(datastoreInformation: IDatastoreInformation, active: Boolean, searchKeys: Set<String>): Flow<String>
	/**
	 * Retrieves all the [Patient.id]s delegated to a data owner (by checking their [searchKeys]) and where [Patient.tags] contains
	 * at least one tag with type [tagType] and code [tagCode]. If [tagCode] is null, the presence of any tag with the specified [tagType]
	 * will suffices.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param dataOwnerId the data owner id.
	 * @param tagType the tag type to search for in [Patient.tags]
	 * @param tagCode the tag code to search for in [Patient.tags] with the type [tagType].
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByDataOwnerTag(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, tagType: String, tagCode: String? = null): Flow<String>
	fun listOfMergesAfter(datastoreInformation: IDatastoreInformation, date: Long?): Flow<Patient>
	suspend fun countByHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Int
	suspend fun countOfHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Int

	/**
	 * Return all the [Patient.id]s with a delegation for [healthcarePartyId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param healthcarePartyId the data owner id or a search key.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Flow<String>

	/**
	 * Retrieves all the [Patient.id]s with a delegation for a data owner given their [searchKeys] where [Patient.dateOfBirth]
	 * is equal to [date].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param date the date of birth.
	 * @param searchKeys the search keys for a data owner (data owner id + access control keys).
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyAndDateOfBirth(datastoreInformation: IDatastoreInformation, date: Int?, searchKeys: Set<String>): Flow<String>

	/**
	 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.dateOfBirth] is between [startDate] (if
	 * provided) and [endDate] (if provided).
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param startDate the upper bound for [Patient.dateOfBirth] as fuzzy date. If null all the patients since the
	 * beginning of time will be retrieved
	 * @param endDate the lower bound for [Patient.dateOfBirth] as fuzzy date. If null all the patients until the end
	 * of time will be retrieved.
	 * @param healthcarePartyId the healthcare party id.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyAndDateOfBirth(datastoreInformation: IDatastoreInformation, startDate: Int?, endDate: Int?, healthcarePartyId: String): Flow<String>

	/**
	 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.gender] is equal to
	 * [gender], [Patient.education] is equal to [education], and [Patient.profession] is equal to [profession].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param healthcarePartyId the id of a data owner or a search key.
	 * @param gender the gender to search. If null, all the results will be returned.
	 * @param education the education to search. If null, the patients then the [profession] parameter will have no effect.
	 * @param profession the profession to search.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyGenderEducationProfession(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, gender: Gender?, education: String?, profession: String?): Flow<String>

	/**
	 * Returns all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.firstName], [Patient.lastName],
	 * [Patient.maidenName], or [Patient.spouseName] start with the specified [searchString].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param searchString the part of the name to search.
	 * @param healthcarePartyId the id of a data owner or a search key.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyNameContainsFuzzy(datastoreInformation: IDatastoreInformation, searchString: String?, healthcarePartyId: String, limit: Int? = null): Flow<String>

	/**
	 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.externalId] starts with
	 * [externalId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param externalId the external id to search.
	 * @param healthcarePartyId the id of a data owner or a search key.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyAndExternalId(datastoreInformation: IDatastoreInformation, externalId: String?, healthcarePartyId: String): Flow<String>

	/**
	 * Retrieves all the [Patient.id]s that have among the available telecoms (that are nested in the [Address] objects
	 * in [Patient.addresses]) there is at least one that starts with [searchString].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchString the search string for the telecom numbers.
	 * @param healthcarePartyId the id of a data owner or a search key.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyAndTelecom(datastoreInformation: IDatastoreInformation, searchString: String?, healthcarePartyId: String): Flow<String>

	/**
	 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where at least an address in [Patient.addresses]
	 * has a concatenation of [Address.street] + [Address.postalCode] + [Address.city] that starts with the specified
	 * [searchString].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchString the string to search in the addresses.
	 * @param healthcarePartyId the id of a data owner or a search key.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyAndAddress(datastoreInformation: IDatastoreInformation, searchString: String?, healthcarePartyId: String): Flow<String>

	/**
	 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where at least an address in [Patient.addresses]
	 * has a concatenation of [Address.street] + [Address.city] that starts with [streetAndCity], where [Address.postalCode]
	 * starts with [postalCode], and where [Address.houseNumber] starts with [houseNumber].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param streetAndCity the string to search in the address street + city.
	 * @param postalCode the postal code to search.
	 * @param houseNumber the house number to search.
	 * @param healthcarePartyId the id of a data owner or a search key.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyAndAddress(datastoreInformation: IDatastoreInformation, streetAndCity: String?, postalCode: String?, houseNumber: String?, healthcarePartyId: String): Flow<String>

	/**
	 * Retrieves all the ids of the [Patient]s for a given healthcare party in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param healthcarePartyId the id of the healthcare party.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the ids.
	 */
	fun findPatientIdsByHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient] entities for a given healthcare party id, sorted by the concatenation of [Patient.lastName] and
	 * [Patient.firstName] normalized removing all the characters that are not letters and mapping all the characters
	 * outside the standard english alphabet to letters of the alphabet.
	 * If a [name] is passed, only the patients which normalized key starts with the normalized [name]
	 * will be returned.
	 * The result will be provided in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param name a prefix that will match the normalized [Patient.lastName] and [Patient.firstName] concatenation.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @param descending whether return the patients sorted by normalized name in descending or ascending order.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [Patient]s.
	 */
	fun findPatientsByHcPartyAndName(datastoreInformation: IDatastoreInformation, name: String?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient]s for which a given healthcare party is responsible, i.e. is in
	 * [Patient.patientHealthCareParties], sorted by the concatenation of [Patient.lastName] and [Patient.firstName]
	 * normalized removing all the characters that are not letters and mapping all the characters outside the standard
	 * english alphabet to letters of the alphabet.
	 * If a [name] is passed, only the patients which normalized key starts with the normalized [name]
	 * will be returned.
	 * The result will be provided in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param name a prefix that will match the normalized [Patient.lastName] and [Patient.firstName] concatenation.
	 * @param pagination a [PaginationOffset] of [String] for pagination.
	 * @param descending whether return the patients sorted by normalized name in descending or ascending order.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [Patient]s.
	 */
	fun findPatientsOfHcPartyAndName(datastoreInformation: IDatastoreInformation, name: String?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient]s for a given healthcare party which [Patient.ssin] matches [ssin] in a format for
	 * pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param ssin the [Patient.ssin] to search. If null, all [Patient]s will be returned.
	 * @param healthcarePartyId the healthcare party id.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @param descending whether to sort the result in descending or ascending order by [Patient.ssin].
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Patient]s.
	 */
	fun findPatientsByHcPartyAndSsin(datastoreInformation: IDatastoreInformation, ssin: String?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient]s for which a given healthcare party is responsible, i.e. is in [Patient.patientHealthCareParties],
	 * and where [Patient.ssin] matches [ssin] in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param ssin the [Patient.ssin] to search. If null, all [Patient]s will be returned.
	 * @param healthcarePartyId the healthcare party id.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @param descending whether to sort the result in descending or ascending order by [Patient.ssin].
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Patient]s.
	 */
	fun findPatientsOfHcPartyAndSsin(datastoreInformation: IDatastoreInformation, ssin: String?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient]s with a delegation for [healthcarePartyId] where [Patient.dateOfBirth] is between [startDate] (if
	 * provided) and [endDate] (if provided) in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param startDate the upper bound for [Patient.dateOfBirth] as fuzzy date. If null all the patients since the
	 * beginning of time will be retrieved
	 * @param endDate the lower bound for [Patient.dateOfBirth] as fuzzy date. If null all the patients until the end
	 * of time will be retrieved.
	 * @param healthcarePartyId the healthcare party id.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @param descending whether to sort the result in descending or ascending order by [Patient.ssin].
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Patient]s.
	 */
	fun findPatientsByHcPartyDateOfBirth(datastoreInformation: IDatastoreInformation, startDate: Int?, endDate: Int?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient]s for which a given healthcare party is responsible, i.e. is in [Patient.patientHealthCareParties],
	 * and where  [Patient.dateOfBirth] is between [startDate] (if provided) and [endDate] (if provided) in a format for
	 * pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param startDate the upper bound for [Patient.dateOfBirth] as fuzzy date. If null all the patients since the
	 * beginning of time will be retrieved
	 * @param endDate the lower bound for [Patient.dateOfBirth] as fuzzy date. If null all the patients until the end
	 * of time will be retrieved.
	 * @param healthcarePartyId the healthcare party id.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @param descending whether to sort the result in descending or ascending order by [Patient.ssin].
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Patient]s.
	 */
	fun findPatientsOfHcPartyDateOfBirth(datastoreInformation: IDatastoreInformation, startDate: Int?, endDate: Int?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	suspend fun findPatientsByUserId(datastoreInformation: IDatastoreInformation, id: String): Patient?
	fun getPatients(datastoreInformation: IDatastoreInformation, patIds: Collection<String>): Flow<Patient>

	suspend fun getPatientByExternalId(datastoreInformation: IDatastoreInformation, externalId: String): Patient?

	/**
	 * Returns all the [Patient]s where [Patient.deletionDate] is after [start] and before [end], if provided, sorted
	 * by [Patient.deletionDate] and provided in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param start the lower bound for [Patient.deletionDate] as timestamp.
	 * @param end the upper bound for [Patient.deletionDate] as timestamp. If null, there will be no lower bound.
	 * @param paginationOffset a [PaginationOffset] of [Long] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Patient]s.
	 */
	fun findDeletedPatientsByDeleteDate(datastoreInformation: IDatastoreInformation, start: Long, end: Long?, descending: Boolean, paginationOffset: PaginationOffset<Long>): Flow<ViewQueryResultEvent>

	fun findDeletedPatientsByNames(datastoreInformation: IDatastoreInformation, firstName: String?, lastName: String?): Flow<Patient>

	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Patient>

	/**
	 * Returns all the [Patient]s where [Patient.modified] is after [date] in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param date the lower bound for [Patient.modified] as timestamp.
	 * @param paginationOffset a [PaginationOffset] of [Long] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Patient]s.
	 */
	fun findPatientsModifiedAfter(datastoreInformation: IDatastoreInformation, date: Long, paginationOffset: PaginationOffset<Long>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient.id]s with a delegation for [healthcarePartyId] where [Patient.ssin] is equal to one of
	 * the provided [ssins].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param ssins the ssin numbers to search.
	 * @param healthcarePartyId a data owner id or a search key.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyAndSsins(datastoreInformation: IDatastoreInformation, ssins: Collection<String>, healthcarePartyId: String): Flow<String>

	@Deprecated(message = "A Data Owner may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	suspend fun getHcPartyKeysForDelegate(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Map<String, String>

	suspend fun getAesExchangeKeysForDelegate(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Map<String, Map<String, Map<String, String>>>

	/**
	 * Retrieves all the [Patient]s for a given healthcare party that appear multiple times in the `by_ssin` view in a
	 * format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param healthcarePartyId the id of the delegated healthcare party.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] pf [ViewQueryResultEvent] containing the [Patient]s.
	 */
	fun getDuplicatePatientsBySsin(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient]s for a given healthcare party that appear multiple times in the `by_name` view in a
	 * format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param healthcarePartyId the id of the delegated healthcare party.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] pf [ViewQueryResultEvent] containing the [Patient]s.
	 */
	fun getDuplicatePatientsByName(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	fun findPatients(datastoreInformation: IDatastoreInformation, ids: Collection<String>): Flow<ViewQueryResultEvent>

	fun findPatients(datastoreInformation: IDatastoreInformation, ids: Flow<String>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient.id]s that have a delegation to a data owner given their [searchKeys] and that have at least
	 * one of the provided [identifiers] in [Patient.identifier].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKeys a [Set] of search keys (access control keys + data owner id).
	 * @param identifiers the identifiers to search.
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>

	/**
	 * Retrieves all the [Patient]s that have a delegation to a data owner given their [searchKeys] and that have at least
	 * one identifier in [Patient.identifier] with the specified [system] and [value].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKeys a [Set] of search keys (access control keys + data owner id).
	 * @param system the identifier system.
	 * @param value the identifier value.
	 * @return a [Flow] of [Patient]s.
	 */
	fun listPatientsByHcPartyAndIdentifier(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, system: String, value: String): Flow<Patient>

	/**
	 * Retrieves all the [Patient]s for a given healthcare party id, sorted by the concatenation of [Patient.lastName] and
	 * [Patient.firstName] normalized removing all the characters that are not letters and mapping all the characters
	 * outside the standard english alphabet to letters of the alphabet.
	 * If a [searchString] is passed, only the patients which normalized key starts with the normalized [searchString]
	 * will be returned.
	 * The result will be provided in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchString a prefix that will match the normalized [Patient.lastName] and [Patient.firstName] concatenation.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @param descending whether return the patients sorted by normalized name in descending or ascending order.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [Patient]s.
	 */
	fun findPatientsByHcPartyNameContainsFuzzy(datastoreInformation: IDatastoreInformation, searchString: String?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient]s for which a given healthcare party is responsible, i.e. is in
	 * [Patient.patientHealthCareParties], sorted by the concatenation of [Patient.lastName] and [Patient.firstName]
	 * normalized removing all the characters that are not letters and mapping all the characters outside the standard
	 * english alphabet to letters of the alphabet.
	 * If a [searchString] is passed, only the patients which normalized key starts with the normalized [searchString]
	 * will be returned.
	 * The result will be provided in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchString a prefix that will match the normalized [Patient.lastName] and [Patient.firstName] concatenation.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @param descending whether return the patients sorted by normalized name in descending or ascending order.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [Patient]s.
	 */
	fun findPatientsOfHcPartyNameContainsFuzzy(datastoreInformation: IDatastoreInformation, searchString: String?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient.id]s with a delegation a data owner (through their data owner id + access control keys),
	 * where [Patient.modified] is not null and greater than or equal to[startDate] (if provided, otherwise not lower
	 * bound will be set), and less than or equal to [endDate] (if provided, otherwise no upper bound will be set).
	 * If the [Patient.modified] timestamp is null, then [Patient.created] will be considered. If also [Patient.created]
	 * is null, then this patient will be considered in the results.
	 * The results will be sorted by [Patient.modified] or [Patient.created] in ascending or descending order according
	 * to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKeys the data owner id + access control keys.
	 * @param startDate the lower bound for the [Patient.modified] timestamp.
	 * @param endDate the upper bound for the [Patient.modified] timestamp.
	 * @param descending whether to sort the result in descending or ascending order by [Patient.modified].
	 * @return a [Flow] of [Patient.id]s.
	 */
	fun listPatientIdsByDataOwnerModificationDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String>
}
