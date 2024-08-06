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
	 * @param hcPartyId the id of the healthcare party.
	 * @param startOpeningDate the timestamp of the start opening date. If null, all the [Contact]s since the beginning of time will be retrieved.
	 * @param endOpeningDate the timestamp of the end opening date. If null, all the [Contact]s until the end of time will be retrieved.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [Contact]s.
	 */
	fun listContactsByOpeningDate(datastoreInformation: IDatastoreInformation, hcPartyId: String, startOpeningDate: Long?, endOpeningDate: Long?, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>
	fun findContactsByHcParty(datastoreInformation: IDatastoreInformation, hcPartyId: String, pagination: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun findContactsByIds(datastoreInformation: IDatastoreInformation, contactIds: Flow<String>): Flow<ViewQueryResultEvent>
	fun findContactsByIds(datastoreInformation: IDatastoreInformation, contactIds: Collection<String>): Flow<ViewQueryResultEvent>
	fun listContactIdsByHealthcareParty(datastoreInformation: IDatastoreInformation, hcPartyId: String): Flow<String>
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

	// endregion

	// region service

	fun findServiceIdsByIdQualifiedLink(datastoreInformation: IDatastoreInformation, ids: List<String>, linkType: String?): Flow<String>
	fun listServiceIdsByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String): Flow<Service>
	fun listServiceIdsByHcParty(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>): Flow<String>
	fun listServiceIdsByTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
	fun listServiceIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>
	fun listServiceIdsByHcPartyHealthElementIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, healthElementIds: List<String>): Flow<String>
	fun listServiceIdsByPatientAndTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
	fun listServiceIdsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
	fun listCodesFrequencies(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String): Flow<Pair<ComplexKey, Long?>>
	fun listServicesIdsByPatientAndCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
	fun listServicesIdsByPatientForeignKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, patientSecretForeignKeys: Set<String>): Flow<String>
	fun listIdsByServices(datastoreInformation: IDatastoreInformation, services: Collection<String>): Flow<ContactIdServiceId>
	fun findServiceIdsByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String): Flow<Service>

	// endregion
}
