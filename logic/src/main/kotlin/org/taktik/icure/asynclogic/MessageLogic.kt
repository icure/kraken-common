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
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.pagination.PaginationElement

interface MessageLogic : EntityPersister<Message, String>, EntityWithSecureDelegationsLogic<Message> {

    /**
     * Retrieves all the [Message]s for a given healthcare party, where [Message.fromAddress] contains [fromAddress],
     * sorted by [Message.received], in a format for pagination.
     *
     * @param partyId the id of the healthcare party.
     * @param fromAddress one of the [Message.fromAddress] to search.
     * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
     * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
     */
    fun findMessagesByFromAddress(
        partyId: String,
        fromAddress: String,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement>

    /**
     * Retrieves all the [Message]s for a given healthcare party, where [Message.toAddresses] contains [toAddress],
     * sorted by [Message.received], in a format for pagination.
     *
     * @param partyId the id of the healthcare party.
     * @param toAddress one of the [Message.toAddresses] to search.
     * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
     * @param reverse whether to sort the result in ascending or descending order by actor.
     * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
     */
	fun findMessagesByToAddress(partyId: String, toAddress: String, paginationOffset: PaginationOffset<ComplexKey>, reverse: Boolean = false): Flow<PaginationElement>

    /**
     * Retrieves all [Message]s for a healthcare party, with the provided [Message.transportGuid] and sorted by
     * [Message.received] in a format for pagination.
     *
     * @param partyId the id of the HCP.
     * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
     * party will be returned. If [transportGuid] ends with the string `:*`, then the asterisk is removed form the start key.
     * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
     * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
     */
    fun findMessagesByTransportGuidReceived(
        partyId: String,
        transportGuid: String?,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement>

    /**
     * Retrieves all [Message]s for a healthcare party, with the provided [Message.transportGuid] in a format for pagination.
     *
     * @param partyId the id of the HCP.
     * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
     * party will be returned. If [transportGuid] ends with the string `:*`, then the asterisk is removed for the start key.
     * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
     * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
     */
    fun findMessagesByTransportGuid(
        partyId: String,
        transportGuid: String?,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement>

    fun listMessageIdsByTransportGuid(hcPartyId: String, transportGuid: String?): Flow<String>

    /**
     * Retrieves all [Message]s for a healthcare party, with the provided [Message.transportGuid] and which [Message.sent]
     * date is between [fromDate] and [toDate] in a format for pagination.
     *
     * @param partyId the id of the HCP.
     * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
     * party will be returned.
     * @param fromDate the lower bound timestamp for [Message.sent].
     * @param toDate the upper bound timestamp for [Message.sent].
     * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
     * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
     */
    fun findMessagesByTransportGuidSentDate(
        partyId: String,
        transportGuid: String,
        fromDate: Long,
        toDate: Long,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement>

    suspend fun addDelegation(message: Message, delegation: Delegation): Message?

    fun createMessages(entities: Collection<Message>): Flow<Message>

    suspend fun createMessage(message: Message): Message?

    suspend fun getMessage(messageId: String): Message?

    /**
     * Retrieves all the [Message]s for the given set of search keys and a set of [Message.secretForeignKeys].
     * Note: if the current user's data owner id is equal to [hcPartyId], then all the search keys available for the
     * user are considered when retrieving the [Message]s.
     *
     * @param hcPartyId the id of the healthcare party.
     * @param secretPatientKeys the secret patient keys to consider.
     * @return a [Flow] of [Message]s matching the criterion.
     */
    fun listMessagesByHCPartySecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<Message>

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
     */
    fun listMessageIdsByDataOwnerPatientSentDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

    fun setStatus(messages: Collection<Message>, status: Int): Flow<Message>
    fun setReadStatus(messages: Collection<Message>, userId: String, status: Boolean, time: Long): Flow<Message>

    /**
     * Finds all the [Message]s related to a specific HCP, sorted by [Message.received], in a format for pagination.
     *
     * @param hcPartyId the id of the HCP.
     * @param paginationOffset a [PaginationOffset] that marks the start of the found messages.
     * @return a [Flow] of [Message]s wrapped in [PaginationElement]s, for pagination.
     */
    fun findForHcPartySortedByReceived(hcPartyId: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

    suspend fun addDelegations(message: Message, delegations: List<Delegation>): Message?
    fun getMessageChildren(messageId: String): Flow<Message>
    fun getMessagesChildren(parentIds: List<String>): Flow<List<Message>>
    fun getMessagesByTransportGuids(hcpId: String, transportGuids: Set<String>): Flow<Message>
    fun listMessagesByInvoiceIds(ids: List<String>): Flow<Message>
    fun listMessagesByExternalRefs(hcPartyId: String, externalRefs: List<String>): Flow<Message>
    fun solveConflicts(limit: Int? = null): Flow<IdAndRev>
    fun filterMessages(
        paginationOffset: PaginationOffset<Nothing>,
        filter: FilterChain<Message>
    ): Flow<ViewQueryResultEvent>
}
