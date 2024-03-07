/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.Identifier

interface PatientDAO : GenericDAO<Patient> {

	fun listPatientIdsByHcPartyAndName(datastoreInformation: IDatastoreInformation, name: String, healthcarePartyId: String): Flow<String>
	fun listPatientIdsOfHcPartyAndName(datastoreInformation: IDatastoreInformation, name: String, healthcarePartyId: String): Flow<String>
	fun listPatientIdsByHcPartyAndSsin(datastoreInformation: IDatastoreInformation, ssin: String, healthcarePartyId: String): Flow<String>
	fun listPatientIdsOfHcPartyAndSsin(datastoreInformation: IDatastoreInformation, ssin: String, healthcarePartyId: String): Flow<String>
	fun listPatientIdsByActive(datastoreInformation: IDatastoreInformation, active: Boolean, searchKeys: Set<String>): Flow<String>
	fun listOfMergesAfter(datastoreInformation: IDatastoreInformation, date: Long?): Flow<Patient>
	suspend fun countByHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Int
	suspend fun countOfHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Int
	fun listPatientIdsByHcParty(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Flow<String>
	fun listPatientIdsByHcPartyAndDateOfBirth(datastoreInformation: IDatastoreInformation, date: Int?, searchKeys: Set<String>): Flow<String>

	fun listPatientIdsByHcPartyAndDateOfBirth(datastoreInformation: IDatastoreInformation, startDate: Int?, endDate: Int?, healthcarePartyId: String): Flow<String>
	fun listPatientIdsByHcPartyGenderEducationProfession(datastoreInformation: IDatastoreInformation, healthcarePartyId: String, gender: Gender?, education: String?, profession: String?): Flow<String>
	fun listPatientIdsForHcPartyDateOfBirth(datastoreInformation: IDatastoreInformation, date: Int?, healthcarePartyId: String): Flow<String>
	fun listPatientIdsByHcPartyNameContainsFuzzy(datastoreInformation: IDatastoreInformation, searchString: String?, healthcarePartyId: String, limit: Int? = null): Flow<String>
	fun listPatientIdsOfHcPartyNameContainsFuzzy(datastoreInformation: IDatastoreInformation, searchString: String?, healthcarePartyId: String, limit: Int?): Flow<String>

	fun listPatientIdsByHcPartyAndExternalId(datastoreInformation: IDatastoreInformation, externalId: String?, healthcarePartyId: String): Flow<String>
	fun listPatientIdsByHcPartyAndTelecom(datastoreInformation: IDatastoreInformation, searchString: String?, healthcarePartyId: String): Flow<String>
	fun listPatientIdsByHcPartyAndAddress(datastoreInformation: IDatastoreInformation, searchString: String?, healthcarePartyId: String): Flow<String>
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

	fun findPatientsByHcPartyModificationDate(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	fun findPatientsOfHcPartyModificationDate(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, healthcarePartyId: String, pagination: PaginationOffset<ComplexKey>, descending: Boolean): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Patient]s for a given healthcare party which [Patient.dateOfBirth] is between [startDate] (if
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

	fun listPatientIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>

	fun listPatientsByHcPartyAndIdentifier(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, system: String, id: String): Flow<Patient>

	/**
	 * Retrieves all the [Patient] entities for a given healthcare party id, sorted by the concatenation of [Patient.lastName] and
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
}
