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
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface MessageService :
	EntityWithSecureDelegationsService<Message>,
	EntityWithConflictResolutionService {

	/**
	 * Retrieves all the [Message]s for a given healthcare party, where [Message.fromAddress] contains [fromAddress],
	 * sorted by [Message.received], in a format for pagination.
	 * This method will filter out all the entities that the current user
	 * cannot access, but it will ensure that the page size specified in the [paginationOffset] will be reached as long
	 * as there are available elements.
	 *
	 * @param hcPartyId the id of the healthcare party.
	 * @param fromAddress one of the [Message.fromAddress] to search.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
	 */
	fun findMessagesByFromAddress(
		hcPartyId: String,
		fromAddress: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement>

	/**
	 * Retrieves all the [Message]s for a given healthcare party, where [Message.toAddresses] contains [toAddress],
	 * sorted by [Message.received], in a format for pagination.
	 * This method will filter out all the entities that the current user
	 * cannot access, but it will ensure that the page size specified in the [paginationOffset] will be reached as long
	 * as there are available elements.
	 *
	 * @param hcPartyId the id of the healthcare party.
	 * @param toAddress one of the [Message.toAddresses] to search.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @param reverse whether to sort the result in ascending or descending order by actor.
	 * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
	 */
	fun findMessagesByToAddress(
		hcPartyId: String,
		toAddress: String,
		paginationOffset: PaginationOffset<ComplexKey>,
		reverse: Boolean?,
	): Flow<PaginationElement>

	/**
	 * Retrieves all [Message]s for a healthcare party, with the provided [Message.transportGuid] and sorted by
	 * [Message.received] in a format for pagination.
	 * This method will filter out all the entities that the current user cannot access, but it will
	 * ensure that the page size specified in the [paginationOffset] will be reached as long as there are available
	 * elements.
	 *
	 * @param hcPartyId the id of the HCP.
	 * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
	 * party will be returned. If [transportGuid] ends with the string `:*`, then the asterisk is removed from the start key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
	 * @throws AccessDeniedException if the current user does not match the precondition to find [Message]s.
	 */
	fun findMessagesByTransportGuidReceived(
		hcPartyId: String,
		transportGuid: String?,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement>

	/**
	 * Retrieves all [Message]s for a healthcare party, with the provided [Message.transportGuid] in a format for pagination.
	 * This method will filter out all the entities that the current user cannot access, but it will
	 * ensure that the page size specified in the [paginationOffset] will be reached as long as there are available
	 * elements.
	 *
	 * @param hcPartyId the id of the HCP.
	 * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
	 * party will be returned. If [transportGuid] ends with the string `:*`, then the asterisk is removed for the start key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
	 * @throws AccessDeniedException if the current user does not match the precondition to find [Message]s.
	 */
	fun findMessagesByTransportGuid(
		hcPartyId: String,
		transportGuid: String?,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement>

	/**
	 * Retrieves all [Message]s for a healthcare party, with the provided [Message.transportGuid] and which [Message.sent]
	 * date is between [fromDate] and [toDate] in a format for pagination.
	 * This method will filter out all the entities that the current user cannot access, but it will
	 * ensure that the page size specified in the [paginationOffset] will be reached as long as there are available
	 * elements.
	 *
	 * @param hcPartyId the id of the HCP.
	 * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
	 * party will be returned.
	 * @param fromDate the lower bound timestamp for [Message.sent].
	 * @param toDate the upper bound timestamp for [Message.sent].
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
	 * @throws AccessDeniedException if the current user does not match the precondition to find [Message]s.
	 */
	fun findMessagesByTransportGuidSentDate(
		hcPartyId: String,
		transportGuid: String,
		fromDate: Long,
		toDate: Long,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement>

	suspend fun addDelegation(messageId: String, delegation: Delegation): Message?

	suspend fun createMessage(message: Message): Message?

	fun createMessages(entities: Collection<Message>): Flow<Message>

	suspend fun getMessage(messageId: String): Message?

	/**
	 * Retrieves a batch of [Message]s by their ids.
	 * This method will filter out all the entities that the current user cannot access.
	 *
	 * @param messageIds the ids of the messages to retrieve.
	 * @return a [Flow] of [Message]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to retrieve [Message]s.
	 */
	fun getMessages(messageIds: List<String>): Flow<Message>

	suspend fun modifyMessage(message: Message): Message?

	/**
	 * Returns all the [Message]s that the current healthcare party can access, given the [Message.secretForeignKeys].
	 * This method will theoretically filter out all the entities that the current user cannot access (but this
	 * should not happen, as we are getting the [Message]s by delegate).
	 * Note: this method will also use the available search keys for the current user to retrieve the results.
	 *
	 * @param secretPatientKeys the secret patient keys.
	 * @return a [Flow] of [Message]s.
	 */
	@Deprecated("This method is inefficient for high volumes of keys, use listMessageIdsByDataOwnerPatientSentDate instead")
	fun listMessagesByCurrentHCPartySecretPatientKeys(secretPatientKeys: List<String>): Flow<Message>

	/**
	 * Retrieves the ids of all the [Message]s given the [dataOwnerId] (plus all the current access keys if that is
	 * equal to the data owner id of the user making the request) and a set of [Message.secretForeignKeys].
	 * Only the ids of the Messages where [Message.sent] is not null are returned and the results are sorted by
	 * [Message.sent] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param dataOwnerId the data owner id.
	 * @param secretForeignKeys a [Set] of [Message.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the Messages where [Message.sent] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the Messages where [Message.sent] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [Message.sent] ascending or descending.
	 * @return a [Flow] of Message ids.
	 * @throws AccessDeniedException if [dataOwnerId] is not the current data owner id and is not among the access keys
	 * and the current user does not have the permission to search Messages for other users.
	 */
	fun listMessageIdsByDataOwnerPatientSentDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	fun setStatus(messageIds: List<String>, status: Int): Flow<Message>
	fun setReadStatus(messageIds: List<String>, userId: String?, status: Boolean, time: Long?): Flow<Message>

	/**
	 * Finds all the [Message]s related to the current data owner, sorted by [Message.received], in a format for pagination.
	 * This method will theoretically filter out all the entities that the current user cannot access (but this
	 * should not happen, as we are getting the [Message]s by delegate) but it will ensure that the page size specified
	 * in the [paginationOffset] will be reached as long as there are available elements.
	 *
	 * @param paginationOffset a [PaginationOffset] that marks the start of the found messages.
	 * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
	 */
	fun findForCurrentHcPartySortedByReceived(paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	suspend fun addDelegations(messageId: String, delegations: List<Delegation>): Message?
	fun getMessageChildren(messageId: String): Flow<Message>
	fun getMessagesChildren(parentIds: List<String>): Flow<Message>
	fun getMessagesByTransportGuids(hcpId: String, transportGuids: Set<String>): Flow<Message>
	fun listMessagesByInvoiceIds(ids: List<String>): Flow<Message>
	override fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev>
	fun filterMessages(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<Message>,
	): Flow<ViewQueryResultEvent>

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [Message].
	 */
	fun deleteMessages(ids: List<IdAndRev>): Flow<Message>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the latest rev of the entity to delete.
	 * @return the deleted [Message].
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteMessage(id: String, rev: String?): Message

	/**
	 * Deletes an entity.
	 * An entity deleted this way can't be restored.
	 * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
	 *
	 * @param id the id of the entity
	 * @param rev the latest known revision of the entity.
	 * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun purgeMessage(id: String, rev: String): DocIdentifier

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity
	 * @param id the id of the entity marked to restore
	 * @param rev the revision of the entity after it was marked as deleted
	 * @return the restored entity
	 */
	suspend fun undeleteMessage(id: String, rev: String): Message

	/**
	 * Retrieves the ids of the [Message]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [Message].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the filter does not specify any data owner id and the current user does not have
	 * the ExtendedRead.Any permission or if the filter specified a data owner id and the current user does not have the
	 * rights to access their data.
	 */
	fun matchMessagesBy(filter: AbstractFilter<Message>): Flow<String>
}
