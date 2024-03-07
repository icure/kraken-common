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
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Service

interface ContactDAO : GenericDAO<Contact> {
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
	 * Retrieves all the [Contact]s for a healthcare party id and secret patient key pair.
	 * The result will be returned in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param hcPartyId the id of the healthcare party.
	 * @param secretPatientKey the secret patient key.
	 * @param pagination a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent]s wrapping the [Contact]s.
	 */
	fun listContactsByHcPartyIdAndPatientSecretKey(datastoreInformation: IDatastoreInformation, hcPartyId: String, secretPatientKey: String, pagination: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>
	fun listContactIdsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<String>
	fun listContactsByHcPartyAndFormId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, formId: String): Flow<Contact>
	fun listContactsByHcPartyAndFormIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, ids: List<String>): Flow<Contact>
	fun listContactsByHcPartyAndServiceId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, serviceId: String): Flow<Contact>
	fun findServiceIdsByIdQualifiedLink(datastoreInformation: IDatastoreInformation, ids: List<String>, linkType: String?): Flow<String>
	fun listServiceIdsByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String): Flow<Service>
	fun listServiceIdsByHcParty(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>): Flow<String>
	fun listServiceIdsByTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
	fun listServiceIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>
	fun listServiceIdsByHcPartyHealthElementIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, healthElementIds: List<String>): Flow<String>
	fun listServiceIdsByPatientAndTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
	fun listServiceIdsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
	fun listContactIdsByTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?): Flow<String>
	fun listContactIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>
	fun listContactIdsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?): Flow<String>
	fun listCodesFrequencies(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String): Flow<Pair<ComplexKey, Long?>>
	fun listServicesIdsByPatientAndCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
	fun listServicesIdsByPatientForeignKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, patientSecretForeignKeys: Set<String>): Flow<String>
	fun listContactsByServices(datastoreInformation: IDatastoreInformation, services: Collection<String>): Flow<Contact>
	fun listIdsByServices(datastoreInformation: IDatastoreInformation, services: Collection<String>): Flow<ContactIdServiceId>
	fun findContactsByExternalId(datastoreInformation: IDatastoreInformation, externalId: String): Flow<Contact>
	fun findServiceIdsByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String): Flow<Service>
	fun findContactsByHcPartyServiceId(datastoreInformation: IDatastoreInformation, hcPartyId: String, serviceId: String): Flow<Contact>
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Contact>
	fun relink(cs: Flow<Contact>): Flow<Contact>
}
