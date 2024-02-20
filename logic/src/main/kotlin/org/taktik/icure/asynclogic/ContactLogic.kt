/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.data.LabelledOccurence
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Service
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.pagination.PaginatedElement

interface ContactLogic : EntityPersister<Contact, String>, EntityWithSecureDelegationsLogic<Contact> {
    suspend fun getContact(id: String): Contact?
    fun getContacts(selectedIds: Collection<String>): Flow<Contact>
    fun findContactsByIds(selectedIds: Collection<String>): Flow<ViewQueryResultEvent>

    /**
     * Retrieves all the [Contact]s for a healthcare party and a set of secret patient keys.
     * Note: if the current data owner is [hcPartyId], then all the search keys available for the user will be applied
     * in retrieving the [Contact]s.
     *
     * @param hcPartyId the id of the healthcare party.
     * @param secretPatientKeys the secret patient keys.
     * @return a [Flow] of [Contact]s.
     */
    fun listContactsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<Contact>

    /**
     * Retrieves all the [Contact]s for a healthcare party id and secret patient key pair.
     * The result will be returned in a format for pagination.
     * Note: differently from [listContactsByHCPartyAndPatient], this method will NOT use all the search keys for the
     * current data owner if the current data owner id is equal to [hcPartyId].
     *
     * @param hcPartyId the id of the healthcare party.
     * @param secretPatientKey the secret patient key.
     * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
     * @return a [Flow] of [PaginatedElement]s wrapping the [Contact]s.
     */
    fun listContactByHCPartyIdAndSecretPatientKey(hcPartyId: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginatedElement>
    fun listContactIdsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<String>

    suspend fun addDelegation(contactId: String, delegation: Delegation): Contact?

    suspend fun createContact(contact: Contact): Contact?

    fun getServices(selectedServiceIds: Collection<String>): Flow<Service>
    fun getServicesLinkedTo(ids: List<String>, linkType: String?): Flow<Service>
    fun listServicesByAssociationId(associationId: String): Flow<Service>

    fun listServiceIdsByHcParty(hcPartyId: String): Flow<String>
    fun listServiceIdsByTag(hcPartyId: String, patientSecretForeignKeys: List<String>?, tagType: String, tagCode: String, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
    fun listServiceIdsByCode(hcPartyId: String, patientSecretForeignKeys: List<String>?, codeType: String, codeCode: String, startValueDate: Long?, endValueDate: Long?, descending: Boolean = false): Flow<String>
    fun listContactIdsByTag(hcPartyId: String, tagType: String, tagCode: String, startValueDate: Long?, endValueDate: Long?): Flow<String>
    fun listServiceIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>): Flow<String>
    fun listContactIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>): Flow<String>
    fun listContactIdsByCode(hcPartyId: String, codeType: String, codeCode: String, startValueDate: Long?, endValueDate: Long?): Flow<String>
    fun listContactIds(hcPartyId: String): Flow<String>
    fun listIdsByServices(services: Collection<String>): Flow<String>
    fun listServicesByHcPartyAndSecretForeignKeys(hcPartyId: String, patientSecretForeignKeys: Set<String>): Flow<String>
    fun listContactsByHcPartyAndFormId(hcPartyId: String, formId: String): Flow<Contact>
    fun listContactsByHcPartyServiceId(hcPartyId: String, serviceId: String): Flow<Contact>
    fun listContactsByExternalId(externalId: String): Flow<Contact>
    fun listServicesByHcPartyAndHealthElementIds(hcPartyId: String, healthElementIds: List<String>): Flow<Service>
    fun listServiceIdsByHcPartyAndHealthElementIds(hcPartyId: String, healthElementIds: List<String>): Flow<String>

    suspend fun getServiceCodesOccurences(
        hcPartyId: String,
        codeType: String,
        minOccurences: Long
    ): List<LabelledOccurence>

    fun listContactsByHcPartyAndFormIds(hcPartyId: String, ids: List<String>): Flow<Contact>
    fun filterContacts(
        paginationOffset: PaginationOffset<Nothing>,
        filter: FilterChain<Contact>
    ): Flow<ViewQueryResultEvent>

    fun filterServices(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<Service>): Flow<Service>

    fun solveConflicts(limit: Int? = null): Flow<IdAndRev>

    /**
     * Retrieves all the [Contact]s that a healthcare party can access and which [Contact.openingDate] is between the
     * [startOpeningDate], if provided, and the [endOpeningDate], if provided.
     * The results will be returned in a format for pagination.
     *
     * @param hcPartyId the id of the healthcare party.
     * @param startOpeningDate the timestamp of the start opening date. If null, all the [Contact]s since the beginning of time will be retrieved.
     * @param endOpeningDate the timestamp of the end opening date. If null, all the [Contact]s until the end of time will be retrieved.
     * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
     * @return a [Flow] of [PaginatedElement] wrapping the [Contact]s.
     */
    fun listContactsByOpeningDate(
        hcPartyId: String,
        startOpeningDate: Long,
        endOpeningDate: Long,
        offset: PaginationOffset<ComplexKey>
    ): Flow<PaginatedElement>

    suspend fun addDelegations(contactId: String, delegations: List<Delegation>): Contact?

    /**
     * Creates [Contact]s in batch.
     * @param contacts a [Flow] of [Contact]s to create.
     * @return a [Flow] containing the created [Contact]s.
     */
    fun createContacts(contacts: Flow<Contact>): Flow<Contact>
}
