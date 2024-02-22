/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Message

interface MessageDAO : GenericDAO<Message> {
	fun listMessagesByFromAddressAndActor(datastoreInformation: IDatastoreInformation, partyId: String, fromAddress: String, actorKeys: List<String>?): Flow<Message>
	fun listMessagesByToAddressAndActor(datastoreInformation: IDatastoreInformation, partyId: String, toAddress: String, actorKeys: List<String>?): Flow<Message>
	fun listMessagesByTransportGuidAndActor(datastoreInformation: IDatastoreInformation, partyId: String, transportGuid: String, actorKeys: List<String>?): Flow<Message>

	/**
	 * Retrieves all the [Message]s for a given healthcare party, where [Message.fromAddress] contains [fromAddress],
	 * sorted by [Message.received], in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param partyId the id of the healthcare party.
	 * @param fromAddress one of the [Message.fromAddress] to search.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @param reverse whether to sort the result in ascending or descending order by actor.
	 * @return a [Flow] of [Message]s wrapped in [ViewQueryResultEvent]s, for pagination.
	 */
	fun listMessagesByFromAddress(datastoreInformation: IDatastoreInformation, partyId: String, fromAddress: String, paginationOffset: PaginationOffset<ComplexKey>, reverse: Boolean = false): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Message]s for a given healthcare party, where [Message.toAddresses] contains [toAddress],
	 * sorted by [Message.received], in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param partyId the id of the healthcare party.
	 * @param toAddress one of the [Message.toAddresses] to search.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @param reverse whether to sort the result in ascending or descending order by actor.
	 * @return a [Flow] of [Message]s wrapped in [ViewQueryResultEvent]s, for pagination.
	 */
	fun findMessagesByToAddress(datastoreInformation: IDatastoreInformation, partyId: String, toAddress: String, paginationOffset: PaginationOffset<ComplexKey>, reverse: Boolean = false): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Message]s for a healthcare party, sorted by [Message.received], in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param partyId the id of the healthcare party.
	 * @param paginationOffset a [PaginationOffset] that marks the start of the found messages.
	 * @return a [Flow] of [Message]s wrapped in [ViewQueryResultEvent]s, for pagination.
	 */
	fun findMessagesByHcPartySortedByReceived(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all [Message]s for a healthcare party, with the provided [Message.transportGuid] in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param partyId the id of the HCP.
	 * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
	 * party will be returned. If [transportGuid] ends with the string `:*`, then the asterisk is removed for the start key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @return a [Flow] of [Message]s wrapped in [ViewQueryResultEvent]s, for pagination.
	 */
	fun findMessagesByTransportGuid(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		transportGuid: String?,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<ViewQueryResultEvent>
	fun listMessageIdsByTransportGuid(datastoreInformation: IDatastoreInformation, hcPartyId: String, transportGuid: String?): Flow<String>

	/**
	 * Retrieves all [Message]s for a healthcare party, with the provided [Message.transportGuid] and sorted by
	 * [Message.received] in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param partyId the id of the HCP.
	 * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
	 * party will be returned. If [transportGuid] ends with the string `:*`, then the asterisk is removed for the start key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @return a [Flow] of [Message]s wrapped in [ViewQueryResultEvent]s, for pagination.
	 */
	fun findMessagesByTransportGuidReceived(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		transportGuid: String?,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all [Message]s for a healthcare party, with the provided [Message.transportGuid] and which [Message.sent]
	 * date is between [fromDate] and [toDate] in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param partyId the id of the HCP.
	 * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
	 * party will be returned.
	 * @param fromDate the lower bound timestamp for [Message.sent].
	 * @param toDate the upper bound timestamp for [Message.sent].
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @return a [Flow] of [Message]s wrapped in [ViewQueryResultEvent]s, for pagination.
	 */
	fun findMessagesByTransportGuidAndSentDate(datastoreInformation: IDatastoreInformation, partyId: String, transportGuid: String, fromDate: Long, toDate: Long, paginationOffset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Message]s for the given set of search keys and a set of [Message.secretForeignKeys].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param searchKeys a [Set] of search keys.
	 * @param secretPatientKeys a [List] of [Message.secretForeignKeys].
	 * @return a [Flow] of [Message]s.
	 */
	fun listMessagesByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<Message>

	/**
	 * Retrieves all the [Message]s for the given healthcare party id and [Message.secretForeignKeys] in a format for
	 * pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param hcPartyId the id of the healthcare party.
	 * @param secretPatientKey a [Message.secretForeignKeys].
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [Message]s wrapped in [ViewQueryResultEvent]s, for pagination.
	 */
	fun listMessagesByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, hcPartyId: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>
	fun getChildren(datastoreInformation: IDatastoreInformation, messageId: String): Flow<Message>
	fun listMessagesByInvoiceIds(datastoreInformation: IDatastoreInformation, invoiceIds: Set<String>): Flow<Message>
	fun getMessagesByTransportGuids(datastoreInformation: IDatastoreInformation, hcPartyId: String, transportGuids: Collection<String>): Flow<Message>
	fun getMessagesByExternalRefs(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, externalRefs: Set<String>): Flow<Message>
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Message>
	fun getMessagesChildren(datastoreInformation: IDatastoreInformation, parentIds: List<String>): Flow<List<Message>>
	fun findMessagesByIds(datastoreInformation: IDatastoreInformation, messageIds: Collection<String>): Flow<ViewQueryResultEvent>
}
