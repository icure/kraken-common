/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.data.LabelledOccurence
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.pagination.PaginationElement

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
     * Retrieves the ids of all the [Contact]s given the [dataOwnerId] (plus all the current access keys if that is
     * equal to the data owner id of the user making the request) and a set of [Contact.secretForeignKeys].
     * Only the ids of the Contacts where [Contact.openingDate] is not null are returned and the results are sorted by
     * [Contact.openingDate] in ascending or descending order according to the [descending] parameter.
     *
     * @param dataOwnerId the id of the data owner.
     * @param secretForeignKeys a [Set] of [Contact.openingDate].
     * @param startDate a fuzzy date. If not null, only the ids of the Contacts where [Contact.openingDate] is greater or equal than [startDate]
     * will be returned.
     * @param endDate a fuzzy date. If not null, only the ids of the Contacts where [Contact.openingDate] is less or equal than [endDate]
     * will be returned.
     * @param descending whether to sort the results by [CalendarItem.startTime] ascending or descending.
     * @return a [Flow] of Contact ids.
     */
    fun listContactIdsByDataOwnerPatientOpeningDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

    suspend fun addDelegation(contactId: String, delegation: Delegation): Contact?

    suspend fun createContact(contact: Contact): Contact?

    fun getServices(selectedServiceIds: Collection<String>): Flow<Service>
    fun getServicesLinkedTo(ids: List<String>, linkType: String?): Flow<Service>
    fun listServicesByAssociationId(associationId: String): Flow<Service>

    fun listIdsByServices(services: Collection<String>): Flow<String>
    fun listContactsByHcPartyAndFormId(hcPartyId: String, formId: String): Flow<Contact>
    fun listContactsByHcPartyServiceId(hcPartyId: String, serviceId: String): Flow<Contact>
    fun listContactsByExternalId(externalId: String): Flow<Contact>
    fun listServicesByHcPartyAndHealthElementIds(hcPartyId: String, healthElementIds: List<String>): Flow<Service>

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

    fun solveConflicts(limit: Int? = null, ids: List<String>? = null): Flow<IdAndRev>

    /**
     * Retrieves all the [Contact]s that a healthcare party can access and which [Contact.openingDate] is between the
     * [startOpeningDate], if provided, and the [endOpeningDate], if provided.
     * The results will be returned in a format for pagination.
     *
     * @param hcPartyId the id of the healthcare party.
     * @param startOpeningDate the timestamp of the start opening date. If null, all the [Contact]s since the beginning of time will be retrieved.
     * @param endOpeningDate the timestamp of the end opening date. If null, all the [Contact]s until the end of time will be retrieved.
     * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
     * @return a [Flow] of [PaginationElement] wrapping the [Contact]s.
     */
    fun listContactsByOpeningDate(
        hcPartyId: String,
        startOpeningDate: Long,
        endOpeningDate: Long,
        offset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement>

    suspend fun addDelegations(contactId: String, delegations: List<Delegation>): Contact?

    /**
     * Creates [Contact]s in batch.
     * @param contacts a [Flow] of [Contact]s to create.
     * @return a [Flow] containing the created [Contact]s.
     */
    fun createContacts(contacts: Flow<Contact>): Flow<Contact>
}
