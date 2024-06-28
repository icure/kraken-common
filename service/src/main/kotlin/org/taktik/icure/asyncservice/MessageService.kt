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
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface MessageService : EntityWithSecureDelegationsService<Message>, EntityWithConflictResolutionService {

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
        paginationOffset: PaginationOffset<ComplexKey>
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
        reverse: Boolean?
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
        paginationOffset: PaginationOffset<ComplexKey>
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
        paginationOffset: PaginationOffset<ComplexKey>
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
        paginationOffset: PaginationOffset<ComplexKey>
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
    fun setReadStatus(messageIds: List<String>, userId: String, status: Boolean, time: Long): Flow<Message>

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
    fun getMessagesChildren(parentIds: List<String>): Flow<List<Message>>
    fun getMessagesByTransportGuids(hcpId: String, transportGuids: Set<String>): Flow<Message>
    fun listMessagesByInvoiceIds(ids: List<String>): Flow<Message>
    fun listMessagesByExternalRefs(hcPartyId: String, externalRefs: List<String>): Flow<Message>
    override fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev>
    fun filterMessages(
        paginationOffset: PaginationOffset<Nothing>,
        filter: FilterChain<Message>
    ): Flow<ViewQueryResultEvent>

    /**
     * Deletes [Message]s in batch.
     * If the user does not meet the precondition to delete [Message]s, an error will be thrown.
     * If the current user does not have the permission to delete one or more elements in
     * the batch, then those elements will not be deleted and no error will be thrown.
     *
     * @param identifiers a [Collection] containing the ids of the [Message]s to delete.
     * @return a [Flow] containing the [DocIdentifier]s of the [Message]s that were successfully deleted.
     */
    fun deleteMessages(identifiers: Collection<String>): Flow<DocIdentifier>

    /**
     * Deletes a [Message].
     *
     * @param id the id of the [Message] to delete.
     * @return a [DocIdentifier] related to the [Message] if the operation completes successfully.
     * @throws [AccessDeniedException] if the current user does not have the permission to delete the [Message].
     * @throws [NotFoundRequestException] if an [Message] with the specified [id] does not exist.
     */
    suspend fun deleteMessage(id: String): DocIdentifier
}
