/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.base.EntityWithConflictResolutionService
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface ContactService: EntityWithSecureDelegationsService<Contact>, EntityWithConflictResolutionService {
	suspend fun getContact(id: String): Contact?
	fun getContacts(selectedIds: Collection<String>): Flow<Contact>
	fun findContactsByIds(selectedIds: Collection<String>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Contact]s for a healthcare party and a set of secret patient keys, filtering out all the
	 * entities that the current user is not allowed to access.
	 * Note: if the current data owner is [hcPartyId], then all the search keys available for the user will be applied
	 * in retrieving the [Contact]s.
	 *
	 * @param hcPartyId the id of the healthcare party.
	 * @param secretPatientKeys the secret patient keys.
	 * @return a [Flow] of [Contact]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to retrieve [Contact]s.
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
	 * @throws AccessDeniedException if [dataOwnerId] is not the current data owner id and is not among the access keys
	 * and the current user does not have the permission to search Calendar Items for other users.
	 */
	fun listContactIdsByDataOwnerPatientOpeningDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	suspend fun addDelegation(contactId: String, delegation: Delegation): Contact?

	suspend fun createContact(contact: Contact): Contact?

	/**
	 * Deletes a batch of [Contact]s.
	 * If the user does not have the permission to delete an [Contact] or the [Contact] does not exist, then it
	 * is ignored without any warning.
	 *
	 * @param ids a [List] containing the ids of the [Contact]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [Contact]s successfully deleted.
	 */
	fun deleteContacts(ids: Set<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [Contact].
	 *
	 * @param id the id of the [Contact] to delete.
	 * @return a [DocIdentifier] related to the [Contact] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [Contact].
	 * @throws [NotFoundRequestException] if an [Contact] with the specified [id] does not exist.
	 */
	suspend fun deleteContact(id: String): DocIdentifier

	suspend fun modifyContact(contact: Contact): Contact?
	suspend fun getService(serviceId: String): Service?
	fun getServices(selectedServiceIds: Collection<String>): Flow<Service>
	fun getServicesLinkedTo(ids: List<String>, linkType: String?): Flow<Service>
	fun listServicesByAssociationId(associationId: String): Flow<Service>

	fun listIdsByServices(services: Collection<String>): Flow<String>
	fun listContactsByHcPartyAndFormId(hcPartyId: String, formId: String): Flow<Contact>
	fun listContactsByHcPartyServiceId(hcPartyId: String, formId: String): Flow<Contact>
	fun listContactsByExternalId(externalId: String): Flow<Contact>
	fun listServicesByHcPartyAndHealthElementIds(hcPartyId: String, healthElementIds: List<String>): Flow<Service>

	suspend fun getServiceCodesOccurences(
		hcPartyId: String,
		codeType: String,
		minOccurences: Long,
	): List<org.taktik.icure.entities.data.LabelledOccurence>

	fun listContactsByHcPartyAndFormIds(hcPartyId: String, ids: List<String>): Flow<Contact>
	fun filterContacts(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<Contact>,
	): Flow<ViewQueryResultEvent>

	fun filterServices(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<Service>): Flow<Service>

	override fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev>

	/**
	 * Retrieves all the [Contact]s that a healthcare party can access and which [Contact.openingDate] is between the
	 * [startOpeningDate], if provided, and the [endOpeningDate], if provided.
	 * The results will be returned in a format for pagination and all the entities that the current user cannot access
	 * will be automatically filtered out from the final result.
	 *
	 * @param hcPartyId the id of the healthcare party.
	 * @param startOpeningDate the timestamp of the start opening date. If null, all the [Contact]s since the beginning of time will be retrieved.
	 * @param endOpeningDate the timestamp of the end opening date. If null, all the [Contact]s until the end of time will be retrieved.
	 * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [Contact]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to search the [Contact]s.
	 */
	fun listContactsByOpeningDate(
		hcPartyId: String,
		startOpeningDate: Long,
		endOpeningDate: Long,
		offset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement>

	suspend fun addDelegations(contactId: String, delegations: List<Delegation>): Contact?

	/**
	 * Creates [Contact]s in batch. To execute this method, a user must have the `Create` permission on the
	 * [Contact] entity.
	 *
	 * @param contacts a [Collection] of [Contact]s to create.
	 * @return a [Flow] containing the created [Contact]s.
	 */
	fun createContacts(contacts: Collection<Contact>): Flow<Contact>

	/**
	 * Creates [Contact]s in batch. To execute this method, a user must have the `Create` permission on the
	 * [Contact] entity.
	 *
	 * @param contacts a [Flow] of [Contact]s to create.
	 * @return a [Flow] containing the created [Contact]s.
	 */
	fun createContacts(contacts: Flow<Contact>): Flow<Contact>

	/**
	 * Updates a batch of [Contact]s. For each element in the batch, it will only apply the modification if it is valid
	 * and the user has the permission to do it, otherwise it will be ignored.
	 *
	 * @param contacts a [Collection] of modified [Contact]s.
	 * @return a [Flow] containing all the [Contact]s that were successfully modified.
	 */
	fun modifyContacts(contacts: Collection<Contact>): Flow<Contact>

	/**
	 * Retrieves the ids of the [Contact]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [Contact].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchContactsBy(filter: AbstractFilter<Contact>): Flow<String>

	/**
	 * Retrieves the ids of the [Service]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [Service].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchServicesBy(filter: AbstractFilter<Service>): Flow<String>
}
