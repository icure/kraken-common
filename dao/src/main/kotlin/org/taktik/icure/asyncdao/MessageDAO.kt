/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Message

interface MessageDAO : GenericDAO<Message> {
	/**
	 * Retrieves all the [Message]s for a given healthcare party, where [Message.fromAddress] is equal to [fromAddress],
	 * sorted by [Message.received], in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param partyId the id of the healthcare party.
	 * @param fromAddress one of the [Message.fromAddress] to search.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] that marks the start of the found messages.
	 * @param reverse whether to sort the result in ascending or descending order.
	 * @return a [Flow] of [Message]s wrapped in [ViewQueryResultEvent]s, for pagination.
	 */
	fun listMessagesByFromAddress(datastoreInformation: IDatastoreInformation, partyId: String, fromAddress: String, paginationOffset: PaginationOffset<ComplexKey>, reverse: Boolean = false): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Message.id]s for a given data owner, where [Message.fromAddress] is equal to [fromAddress].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param dataOwnerId a data owner id or a search key.
	 * @param fromAddress one of the [Message.fromAddress] to search.
	 * @return a [Flow] of [Message.id]s.
	 */
	fun listMessageIdsByFromAddress(datastoreInformation: IDatastoreInformation, dataOwnerId: String, fromAddress: String): Flow<String>

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
	 * Retrieves all the [Message.id]s for a given data owner, where [Message.toAddresses] contains [toAddress].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param dataOwnerId a data owner id or a search key.
	 * @param toAddress one of the [Message.toAddresses] to search.
	 * @return a [Flow] of [Message.id]s.
	 */
	fun listMessageIdsByToAddress(datastoreInformation: IDatastoreInformation, dataOwnerId: String, toAddress: String): Flow<String>

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
		paginationOffset: PaginationOffset<ComplexKey>,
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
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<ViewQueryResultEvent>

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
		paginationOffset: PaginationOffset<ComplexKey>,
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
	@Deprecated("This method is inefficient for high volumes of keys, use listMessageIdsByDataOwnerPatientSentDate instead")
	fun listMessagesByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<Message>

	/**
	 * Retrieves the ids of all the [Message]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of [Message.secretForeignKeys].
	 * Only the ids of the Messages where [Message.sent] is not null are returned and the results are sorted by
	 * [Message.sent] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [Message.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the Messages where [Message.sent] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the Messages where [Message.sent] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [Message.sent] ascending or descending.
	 * @return a [Flow] of Message ids.
	 */
	fun listMessageIdsByDataOwnerPatientSentDate(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>
	fun getChildren(datastoreInformation: IDatastoreInformation, messageId: String): Flow<Message>

	/**
	 * Retrieves all the [Message]s where [Message.invoiceIds] contains at least one of the provided [invoiceIds].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param invoiceIds a [List] of message invoice ids.
	 * @return a [Flow] of [Message]s.
	 */
	fun listMessagesByInvoiceIds(datastoreInformation: IDatastoreInformation, invoiceIds: Set<String>): Flow<Message>
	fun getMessagesByTransportGuids(datastoreInformation: IDatastoreInformation, hcPartyId: String, transportGuids: Collection<String>): Flow<Message>
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Message>

	/**
	 * Returns all the [Message]s where [Message.parentId] is among the provided [parentIds].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param parentIds a [List] of message parent ids.
	 * @return a [Flow] of [Message]s.
	 */
	fun getMessagesChildren(datastoreInformation: IDatastoreInformation, parentIds: List<String>): Flow<Message>
	fun findMessagesByIds(datastoreInformation: IDatastoreInformation, messageIds: Collection<String>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves all the [Message.id]s that have a delegation for [dataOwnerId] and where [Message.transportGuid] is equal
	 * to [transportGuid], sorted by [Message.received]
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param dataOwnerId the data owner id.
	 * @param transportGuid the transport guid to search.
	 * @param descending whether to sort the results in ascending or descending order by [Message.received]
	 * @return a [Flow] of [Message.id]s.
	 */
	fun listMessageIdsByTransportGuidReceived(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		transportGuid: String,
		descending: Boolean,
	): Flow<String>

	/**
	 * Retrieves all [Message.id]s delegated to a data owner, with the provided [Message.transportGuid] and which [Message.sent]
	 * date is between [fromDate] and [toDate], sorted by [Message.sent] in ascending or descending order according to
	 * the value of the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param dataOwnerId the id of the HCP.
	 * @param transportGuid the transport guid to search. If null, all the [Message]s for the specified healthcare
	 * party will be returned.
	 * @param fromDate the lower bound timestamp for [Message.sent].
	 * @param toDate the upper bound timestamp for [Message.sent].
	 * @return a [Flow] of [Message.id]s.
	 */
	fun listMessageIdsByTransportGuidAndSentDate(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		transportGuid: String,
		fromDate: Long?,
		toDate: Long?,
		descending: Boolean,
	): Flow<String>

	/**
	 * Returns all the [Message.id]s where [Message.parentId] is among the provided [parentIds].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param parentIds a [List] of message parent ids.
	 * @return a [Flow] of [Message.id]s.
	 */
	fun listMessageIdsByParents(datastoreInformation: IDatastoreInformation, parentIds: List<String>): Flow<String>

	/**
	 * Retrieves all the [Message.id]s where [Message.invoiceIds] contains at least one of the provided [invoiceIds].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group id and CouchDB instance.
	 * @param invoiceIds a [Set] of message invoice ids.
	 * @return a [Flow] of [Message.id]s.
	 */
	fun listMessageIdsByInvoiceIds(datastoreInformation: IDatastoreInformation, invoiceIds: Set<String>): Flow<String>

	/**
	 * Retrieves all the [Message.id]s with a delegation for the specified [searchKey], where the max among [Message.created],
	 * [Message.modified], and [Message.deletionDate] is greater or equal than [startTimestamp] (if provided) and less or equal
	 * than [endTimestamp] (if provided).
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKey a data owner id or a search key.
	 * @param startTimestamp the lower bound for the latest update date.
	 * @param endTimestamp the upper bound for the latest update date.
	 * @param descending whether to sort the results by last update in ascending or descending order.
	 * @return a [Flow] of [Message.id]s.
	 */
	fun listMessageIdsByDataOwnerLifecycleBetween(datastoreInformation: IDatastoreInformation, searchKey: String, startTimestamp: Long?, endTimestamp: Long?, descending: Boolean): Flow<String>
}
