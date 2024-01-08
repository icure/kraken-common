/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Service
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.exceptions.NotFoundRequestException

interface ContactService: EntityWithSecureDelegationsService<Contact> {
	suspend fun getContact(id: String): Contact?
	fun getContacts(selectedIds: Collection<String>): Flow<Contact>
	fun findContactsByIds(selectedIds: Collection<String>): Flow<ViewQueryResultEvent>
	fun listContactsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<Contact>
	fun listContactIdsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<String>

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

	fun listServiceIdsByHcParty(hcPartyId: String): Flow<String>
	fun listServiceIdsByTag(hcPartyId: String, patientSecretForeignKeys: List<String>?, tagType: String, tagCode: String, startValueDate: Long?, endValueDate: Long?): Flow<String>
	fun listServiceIdsByCode(hcPartyId: String, patientSecretForeignKeys: List<String>?, codeType: String, codeCode: String, startValueDate: Long?, endValueDate: Long?): Flow<String>
	fun listContactIdsByTag(hcPartyId: String, tagType: String, tagCode: String, startValueDate: Long?, endValueDate: Long?): Flow<String>
	fun listServiceIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>): Flow<String>
	fun listContactIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>): Flow<String>
	fun listContactIdsByCode(hcPartyId: String, codeType: String, codeCode: String, startValueDate: Long?, endValueDate: Long?): Flow<String>
	fun listContactIds(hcPartyId: String): Flow<String>
	fun listIdsByServices(services: Collection<String>): Flow<String>
	fun listServicesByHcPartyAndSecretForeignKeys(hcPartyId: String, patientSecretForeignKeys: Set<String>): Flow<String>
	fun listContactsByHcPartyAndFormId(hcPartyId: String, formId: String): Flow<Contact>
	fun listContactsByHcPartyServiceId(hcPartyId: String, formId: String): Flow<Contact>
	fun listContactsByExternalId(externalId: String): Flow<Contact>
	fun listServicesByHcPartyAndHealthElementIds(hcPartyId: String, healthElementIds: List<String>): Flow<Service>
	fun listServiceIdsByHcPartyAndHealthElementIds(hcPartyId: String, healthElementIds: List<String>): Flow<String>

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

	fun solveConflicts(limit: Int? = null): Flow<IdAndRev>
	fun listContactsByOpeningDate(
		hcPartyId: String,
		startOpeningDate: Long,
		endOpeningDate: Long,
		offset: PaginationOffset<List<String>>,
	): Flow<ViewQueryResultEvent>

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
	 * @return a [Flow] containing all the [Contact]s that were succesfully modified.
	 */
	fun modifyContacts(contacts: Collection<Contact>): Flow<Contact>
}
