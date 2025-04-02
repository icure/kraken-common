/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.ContactIdServiceId
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.SubContact

interface ContactDAO : GenericDAO<Contact> {

	// region contact

	suspend fun getContact(datastoreInformation: IDatastoreInformation, id: String): Contact?
	fun getContacts(datastoreInformation: IDatastoreInformation, contactIds: Collection<String>): Flow<Contact>
	fun getContacts(datastoreInformation: IDatastoreInformation, contactIds: Flow<String>): Flow<Contact>

	/**
	 * Retrieves all the [Contact]s for a healthcare party and which [Contact.openingDate] is between the
	 * [startOpeningDate], if provided, and the [endOpeningDate], if provided.
	 * The results will be returned in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param hcPartyId the id of the healthcare party or a search key.
	 * @param startOpeningDate the timestamp of the start opening date. If null, all the [Contact]s since the beginning of time will be retrieved.
	 * @param endOpeningDate the timestamp of the end opening date. If null, all the [Contact]s until the end of time will be retrieved.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [Contact]s.
	 */
	fun listContactsByOpeningDate(datastoreInformation: IDatastoreInformation, hcPartyId: String, startOpeningDate: Long?, endOpeningDate: Long?, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the ids of the [Contact]s for a healthcare party and which [Contact.openingDate] is between the
	 * [startOpeningDate], if provided, and the [endOpeningDate], if provided.
	 * The results will be returned in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param hcPartyId the id of the healthcare party or a search key.
	 * @param startOpeningDate the timestamp of the start opening date. If null, all the [Contact]s since the beginning of time will be retrieved.
	 * @param endOpeningDate the timestamp of the end opening date. If null, all the [Contact]s until the end of time will be retrieved.
	 * @param descending whether to return the results in descending or ascending order by [Contact.openingDate].
	 * @return a [Flow] of [Contact.id]s.
	 */
	fun listContactIdsByOpeningDate(
		datastoreInformation: IDatastoreInformation,
		hcPartyId: String,
		startOpeningDate: Long?,
		endOpeningDate: Long?,
		descending: Boolean
	): Flow<String>
	fun findContactsByHcParty(datastoreInformation: IDatastoreInformation, hcPartyId: String, pagination: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun findContactsByIds(datastoreInformation: IDatastoreInformation, contactIds: Flow<String>): Flow<ViewQueryResultEvent>
	fun findContactsByIds(datastoreInformation: IDatastoreInformation, contactIds: Collection<String>): Flow<ViewQueryResultEvent>
	fun listContactIdsByHealthcareParty(datastoreInformation: IDatastoreInformation, hcPartyId: String): Flow<String>

	@Deprecated("This method is inefficient for high volumes of keys, use listContactIdsByDataOwnerPatientOpeningDate instead")
	fun listContactsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<Contact>

	/**
	 * Retrieves the ids of all the [Contact.id]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of
	 * [Contact.secretForeignKeys].
	 * Only the ids of the Contacts where [Contact.openingDate] is not null are returned and the results are sorted by
	 * [Contact.openingDate] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [Contact.openingDate].
	 * @param startDate a fuzzy date. If not null, only the ids of the Contacts where [Contact.openingDate] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the Contacts where [Contact.openingDate] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [CalendarItem.startTime] ascending or descending.
	 * @return a [Flow] of [Contact.id]s.
	 */
	fun listContactIdsByDataOwnerPatientOpeningDate(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
	fun listContactIdsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<String>

	/**
	 * Retrieves the ids of all the [Contact]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a [formId] existing
	 * in one of the [Contact.subContacts] or [Contact.services].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param formId the form id to search in the sub-contacts and services.
	 * @return a [Flow] of [Contact]s.
	 */
	fun listContactsByHcPartyAndFormId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, formId: String): Flow<Contact>

	/**
	 * Retrieves the ids of all the [Contact]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of
	 * [ids], that are the form ids that can be set in the [Contact.subContacts] and [Contact.services] of this contact.
	 * This method will return a Contact if at least one of his sub-contacts or services has one of the specified
	 * ids.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param ids the list of form ids to search in the sub-contacts and services.
	 * @return a [Flow] of [Contact]s.
	 */
	fun listContactsByHcPartyAndFormIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, ids: List<String>): Flow<Contact>

	/**
	 * Retrieves the ids of all the [Contact.id]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of
	 * [formIds], that are the form ids that can be set in the [Contact.subContacts] and [Contact.services] of this contact.
	 * This method will return a Contact if at least one of his sub-contacts or services has one of the specified
	 * ids.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param formIds the list of form ids to search in the sub-contacts and services.
	 * @return a [Flow] of [Contact.id]s.
	 */
	fun listContactIdsByDataOwnerAndFormIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, formIds: List<String>): Flow<String>
	fun listContactsByHcPartyAndServiceId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, serviceId: String): Flow<Contact>
	fun listContactIdsByTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?): Flow<String>

	/**
	 * Retrieves the ids of all the [Contact.id]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of
	 * [Contact.identifier].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param identifiers the identifiers to search. All the [Contact]s with at least one of this identifiers will be returned.
	 * @return a [Flow] of [Contact.id]s.
	 */
	fun listContactIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>
	fun listContactIdsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?): Flow<String>
	fun listContactsByServices(datastoreInformation: IDatastoreInformation, services: Collection<String>): Flow<Contact>

	/**
	 * Retrieves all the [Contact]s where [Contact.externalId] is equal to [externalId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param externalId the [Contact.externalId] to search.
	 * @return a [Flow] of [Contact]s.
	 */
	fun findContactsByExternalId(datastoreInformation: IDatastoreInformation, externalId: String): Flow<Contact>

	/**
	 * Retrieves all the [Contact.id]s of the Contacts where [Contact.externalId] is equal to [externalId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param externalId the [Contact.externalId] to search.
	 * @return a [Flow] of [Contact.id]s.
	 */
	fun listContactIdsByExternalId(datastoreInformation: IDatastoreInformation, externalId: String): Flow<String>
	fun findContactsByHcPartyServiceId(datastoreInformation: IDatastoreInformation, hcPartyId: String, serviceId: String): Flow<Contact>
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Contact>
	fun relink(cs: Flow<Contact>): Flow<Contact>

	/**
	 * Retrieves the ids of all the [Contact]s with a delegation for the specified [hcPartyId] that have in [Contact.services]
	 * at least one service that has a tag with [tagType] and [tagCode] (only if provided, otherwise all the code stubs
	 * with type [tagType] will match).
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param hcPartyId the id of a data owner or a search key.
	 * @param tagType the type of stub in [Service.tags].
	 * @param tagCode the code of stub in [Service.tags].
	 * @return a [Flow] or [Contact.id]s.
	 */
	fun listContactIdsByServiceTag(
		datastoreInformation: IDatastoreInformation,
		hcPartyId: String,
		tagType: String?,
		tagCode: String?,
	): Flow<String>

	/**
	 * Retrieves the ids of all the [Contact]s with a delegation for the specified [hcPartyId] that have in [Contact.services]
	 * at least one service that has a code with [codeType] and [codeCode] (only if provided, otherwise all the code stubs
	 * with type [codeType] will match).
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param hcPartyId the id of a data owner or a search key.
	 * @param codeType the type of stub in [Service.codes].
	 * @param codeCode the code of stub in [Service.codes].
	 * @return a [Flow] or [Service.id]s.
	 */
	fun listContactIdsByServiceCode(
		datastoreInformation: IDatastoreInformation,
		hcPartyId: String,
		codeType: String,
		codeCode: String?
	): Flow<String>

	// endregion

	// region service

	/**
	 * Retrieves the [Service.id]s that have in [Service.qualifiedLinks] at least one value among [linkValues]. If
	 * [linkQualification] is not null, then the [Service.id] will be returned only if the value corresponds to [linkQualification] in
	 * the map.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param linkValues a list of values of [Service.qualifiedLinks] to search.
	 * @param linkQualification a key of [Service.qualifiedLinks] where the [linkValues] should be.
	 * @return a [Flow] of [Service.id]s.
	 */
	fun findServiceIdsByIdQualifiedLink(datastoreInformation: IDatastoreInformation, linkValues: List<String>, linkQualification: String?): Flow<String>

	/**
	 * Retrieves the [Service.id]s that have in [Service.qualifiedLinks], for any type of qualification, an association
	 * key equal to [associationId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param associationId an association id (i.e. a key of the map that is the value of each entry of [Service.qualifiedLinks]).
	 * @return a [Flow] of [Service.id]s.
	 */
	fun listServiceIdsByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String): Flow<String>

	/**
	 * Retrieves all the [Service.id]s with a delegation for a data owner, given their [searchKeys].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys the search keys of a data owner (data owner id + access control keys).
	 * @return a [Flow] of [Service.id]s.
	 */
	fun listServiceIdsByHcParty(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>): Flow<String>

	/**
	 * Retrieves the ids of all the [Service]s with a delegation for the specified [hcPartyId].
	 * If either [tagType] or [tagCode] are specified, then only the services with a stub in [Service.tags] with the
	 * specified [tagType] and [tagCode] will be returned.
	 * If either [startValueDate] or [endValueDate] are specified, only the Services where [Service.valueDate] (or
	 * [Service.openingDate] if value date is null) is in the specified interval.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param hcPartyId the id of a data owner or a search key.
	 * @param tagType the type of stub in [Service.tags].
	 * @param tagCode the code of stub in [Service.tags].
	 * @param startValueDate if specified, defines the lower bound for [Service.valueDate] or [Service.openingDate].
	 * @param endValueDate if specified, defines the upper bound for [Service.valueDate] or [Service.openingDate].
	 * @param descending whether to return the values in ascending or descending order.
	 * @return a [Flow] or [Service.id]s.
	 */
	fun listServiceIdsByTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>

	/**
	 * Retrieves all the [Service.id]s that have a delegation to a data owner given their [searchKeys] and that have at least
	 * one of the provided [identifiers] in [Service.identifier].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKeys a [Set] of search keys (access control keys + data owner id).
	 * @param identifiers the identifiers to search.
	 * @return a [Flow] of [Service.id]s.
	 */
	fun listServiceIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>

	/**
	 * Retrieves all the [Service.id]s with a delegation for a data owner through their [searchKeys], where
	 * service belongs to a [SubContact] where [SubContact.healthElementId] is among the provided [healthElementIds].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys the search keys of a data owner (data owner id + access control keys).
	 * @param healthElementIds the health element ids to search.
	 * @return a [Flow] of [Service.id]s.
	 */
	fun listServiceIdsByHcPartyHealthElementIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, healthElementIds: List<String>): Flow<String>

	/**
	 * Retrieves all the [Service.id]s with a delegation for the specified [hcPartyId] and associated to the patient
	 * through the provided [patientSecretForeignKeys].
	 * If either [tagType] or [tagCode] are specified, then only the services with a stub in [Service.tags] with the
	 * specified [tagType] and [tagCode] will be returned.
	 * If either [startValueDate] or [endValueDate] are specified, only the Services where [Service.valueDate] (or
	 * [Service.openingDate] if value date is null) is in the specified interval.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param hcPartyId the id of a data owner or a search key.
	 * @param tagType the type of stub in [Service.tags].
	 * @param tagCode the code of stub in [Service.tags].
	 * @param startValueDate if specified, defines the lower bound for [Service.valueDate] or [Service.openingDate].
	 * @param endValueDate if specified, defines the upper bound for [Service.valueDate] or [Service.openingDate].
	 * @param descending whether to return the values in ascending or descending order.
	 * @return a [Flow] or [Service.id]s.
	 */
	fun listServiceIdsByPatientAndTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>

	/**
	 * Retrieves the ids of all the [Service]s with a delegation for the specified [hcPartyId].
	 * If either [codeType] or [codeCode] are specified, then only the services with a stub in [Service.codes] with the
	 * specified [codeType] and [codeCode] will be returned.
	 * If either [startValueDate] or [endValueDate] are specified, only the Services where [Service.valueDate] (or
	 * [Service.openingDate] if value date is null) is in the specified interval.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param hcPartyId the id of a data owner or a search key.
	 * @param codeType the type of stub in [Service.codes].
	 * @param codeCode the code of stub in [Service.codes].
	 * @param startValueDate if specified, defines the lower bound for [Service.valueDate] or [Service.openingDate].
	 * @param endValueDate if specified, defines the upper bound for [Service.valueDate] or [Service.openingDate].
	 * @param descending whether to return the values in ascending or descending order.
	 * @return a [Flow] or [Service.id]s.
	 */
	fun listServiceIdsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
	fun listCodesFrequencies(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String): Flow<Pair<ComplexKey, Long?>>

	/**
	 * Retrieves all the [Service.id]s with a delegation for the specified [hcPartyId] and associated to the patient
	 * through the provided [patientSecretForeignKeys].
	 * If either [codeType] or [codeCode] are specified, then only the services with a stub in [Service.codes] with the
	 * specified [codeType] and [codeCode] will be returned.
	 * If either [startValueDate] or [endValueDate] are specified, only the Services where [Service.valueDate] (or
	 * [Service.openingDate] if value date is null) is in the specified interval.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param hcPartyId the id of a data owner or a search key.
	 * @param codeType the type of stub in [Service.codes].
	 * @param codeCode the code of stub in [Service.codes].
	 * @param startValueDate if specified, defines the lower bound for [Service.valueDate] or [Service.openingDate].
	 * @param endValueDate if specified, defines the upper bound for [Service.valueDate] or [Service.openingDate].
	 * @param descending whether to return the values in ascending or descending order.
	 * @return a [Flow] or [Service.id]s.
	 */
	fun listServicesIdsByPatientAndCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>

	/**
	 * Retrieves all the [Service.id]s with a delegation for a data owner through their [searchKeys] and associated to
	 * the patient given the [patientSecretForeignKeys].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (access control keys + data owner id).
	 * @param patientSecretForeignKeys a [Set] of secret foreign keys.
	 * @return a [Flow] of [Service.id]s.
	 */
	fun listServicesIdsByPatientForeignKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, patientSecretForeignKeys: Set<String>): Flow<String>
	fun listIdsByServices(datastoreInformation: IDatastoreInformation, services: Collection<String>): Flow<ContactIdServiceId>

	/**
	 * Retrieves the [Service]s that have in [Service.qualifiedLinks], for any type of qualification, an association
	 * key equal to [associationId].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param associationId an association id (i.e. a key of the map that is the value of each entry of [Service.qualifiedLinks]).
	 * @return a [Flow] of [Service]s.
	 */
	fun listServicesByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String): Flow<Service>

	/**
	 * Retrieves the ids of the [Service]s with a delegation for a data owner (given their search keys) and linked to a
	 * patient (given their secretForeignKeys).
	 * If the [startDate] fuzzy date is not null, only the [Service]s where [Service.valueDate] (or [Service.openingDate] if
	 * the value date is null) is greater than or equal to [startDate] will be returned.
	 * If the [endDate] fuzzy date is not null, only the [Service]s where [Service.valueDate] (or [Service.openingDate] if
	 * the value date is null) is less than or equal to [startDate] will be returned.
	 * The results will be sorted by [Service.valueDate] (or [Service.openingDate]) in descending or ascending order
	 * according to [descending].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (access control keys + data owner id).
	 * @param patientSecretForeignKeys a [Set] of secret foreign keys.
	 * @param startDate the minimum fuzzy date.
	 * @param endDate the maximum fuzzy date.
	 * @param descending whether to return the results in descending or ascending order by [Service.valueDate] or
	 * [Service.openingDate].
	 * @return a [Flow] of [Service.id]s.
	 */
	fun listServiceIdsByDataOwnerPatientDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		patientSecretForeignKeys: List<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String>
	// endregion
}
