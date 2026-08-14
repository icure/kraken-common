package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.entities.requests.ExchangeDataPieceCreationRequest
import org.taktik.icure.pagination.MultiKeyPaginationElement
import org.taktik.icure.pagination.PaginationElement

interface ExchangeDataService {
	// TODO standard entity persister

	/**
	 * Get the exchange data with the provided exchange data id.
	 * @param id id of the exchange data
	 * @return the exchange data with the provided id if it exists.
	 */
	suspend fun getExchangeDataById(id: String): ExchangeData?
	fun getExchangeDataByIds(ids: List<String>): Flow<ExchangeData>

	/**
	 * Get the exchange data where the delegator and/or delegate is the provided data owner.
	 * Since a certain data owner may have thousands of exchange data this method allows to
	 * retrieve exchange data in multiple pages.
	 * @param dataOwnerId id of a data owner.
	 * @param paginationOffset data for the paged retrieval of data.
	 * @return the events resulting from the DB interrogation.
	 */
	fun findExchangeDataByParticipant(
		dataOwnerId: String,
		paginationOffset: PaginationOffset<String>,
	): Flow<PaginationElement>

	/**
	 * Get the exchange data for a specific delegator->delegate pair. Note that this does not
	 * include delegate->delegator exchange data.
	 * Normally for each delegator-delegate pair there should be only few (< 10) instances
	 * of exchange data, so there is no need to retrieve data in multiple pages.
	 * @param delegatorId id of a data owner.
	 * @param delegateId id of a data owner, potentially the same as [delegatorId].
	 * @return all exchange data where [ExchangeData.delegator] is [delegatorId] and
	 * [ExchangeData.delegate] is [delegateId].
	 */
	fun findExchangeDataByDelegatorDelegatePair(
		delegatorId: String,
		delegateId: String,
	): Flow<ExchangeData>

	/**
	 * Creates new exchange data.
	 * @param exchangeData the exchange data to create.
	 * @return the created exchange data, with updated revision number.
	 */
	suspend fun createExchangeData(exchangeData: ExchangeData): ExchangeData

	/**
	 * Creates new exchange datas.
	 * @param exchangeDatas the exchange datas to create.
	 * @return the created exchange datas, with updated revision number, errors have been filtered out.
	 */
	suspend fun createExchangeDatas(exchangeDatas: List<ExchangeData>): Flow<ExchangeData>

	/**
	 * Modifies existing exchange data.
	 * @param exchangeData the updated exchange data.
	 * @return the updated exchange data, with updated revision number.
	 */
	suspend fun modifyExchangeData(exchangeData: ExchangeData): ExchangeData

	/**
	 * Get the ids of all delegates in exchange data where the data owner is delegator and all delegators in exchange
	 * data where the data owner is delegate. Return only counterparts that are data owners of the specified type.
	 * @param dataOwnerId id of a data owner.
	 * @param counterpartsType data owners types for counterparts which will be returned.
	 * @param ignoreOnEntryForFingerprint if not null ignore the exchange data where there is an entry for the provided
	 * fingerprint.
	 * @return the ids of all data owners in exchange data with the current data owner that are one of the specified
	 * types.
	 * @throws IllegalArgumentException if counterpartTypes is empty.
	 */
	fun getParticipantCounterparts(dataOwnerId: String, counterpartsType: List<DataOwnerType>, ignoreOnEntryForFingerprint: String?): Flow<String>

	/**
	 * Get the pieces of an exchange data group whose [ExchangeData.recipient] is in [filterRecipients], or the single
	 * exchange data with [exchangeDataOrGroupId] as its [ExchangeData.id] if there is no exchange data with that
	 * [ExchangeData.exchangeDataGroupId]. [ExchangeData.recipient] is null exactly for the exchange data with no
	 * [ExchangeData.exchangeDataGroupId] (a plain exchange data, or the exchange data of a parent-type group): a
	 * request for [exchangeDataOrGroupId] that falls back to a plain exchange data can therefore only ever match it if
	 * `null` is an entry of [filterRecipients].
	 *
	 * # Pagination
	 * For the first page call with [startDocumentId] `null`. Whenever the flow emits a
	 * [MultiKeyPaginationElement.NextPage] instead of a [MultiKeyPaginationElement.Row], ask for the next page by
	 * calling again with the same [exchangeDataOrGroupId], [MultiKeyPaginationElement.NextPage.nextKeys] as
	 * [filterRecipients], and [MultiKeyPaginationElement.NextPage.nextDocId] as [startDocumentId]. All results have
	 * been returned once the flow completes without ever emitting a [MultiKeyPaginationElement.NextPage].
	 * @throws IllegalArgumentException when the returned flow is collected, if [filterRecipients] is empty or contains
	 * duplicates.
	 */
	fun findExchangeDataGroupByIdForRecipients(
		exchangeDataOrGroupId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>>

	/**
	 * Get the exchange data where the provided data owner is the delegator and/or delegate for the exchange data,
	 * limited to the exchange data whose [ExchangeData.recipient] is in [filterRecipients]. [ExchangeData.recipient]
	 * is null exactly for the exchange data with no [ExchangeData.exchangeDataGroupId] (a plain exchange data, or the
	 * exchange data of a parent-type group): include `null` as an entry of [filterRecipients] to also get that
	 * exchange data back.
	 *
	 * # Pagination
	 * For the first page call with [startDocumentId] `null`. Whenever the flow emits a
	 * [MultiKeyPaginationElement.NextPage] instead of a [MultiKeyPaginationElement.Row], ask for the next page by
	 * calling again with the same [dataOwnerId], [MultiKeyPaginationElement.NextPage.nextKeys] as [filterRecipients],
	 * and [MultiKeyPaginationElement.NextPage.nextDocId] as [startDocumentId]. All results have been returned once the
	 * flow completes without ever emitting a [MultiKeyPaginationElement.NextPage].
	 * @throws IllegalArgumentException when the returned flow is collected, if [filterRecipients] is empty or contains
	 * duplicates.
	 */
	fun findExchangeDataByParticipantForRecipients(
		dataOwnerId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>>

	/**
	 * Get the exchange data where [ExchangeData.delegator] is [delegatorId] and [ExchangeData.delegate] is
	 * [delegateId], limited to the exchange data whose [ExchangeData.recipient] is in [filterRecipients].
	 * [ExchangeData.recipient] is null exactly for the exchange data with no [ExchangeData.exchangeDataGroupId] (a
	 * plain exchange data, or the exchange data of a parent-type group): include `null` as an entry of
	 * [filterRecipients] to also get that exchange data back.
	 *
	 * Note that, unlike [findExchangeDataByDelegatorDelegatePair], this does not include the exchange data in the
	 * opposite direction, i.e. from [delegateId] to [delegatorId].
	 *
	 * # Pagination
	 * For the first page call with [startDocumentId] `null`. Whenever the flow emits a
	 * [MultiKeyPaginationElement.NextPage] instead of a [MultiKeyPaginationElement.Row], ask for the next page by
	 * calling again with the same [delegatorId] and [delegateId], [MultiKeyPaginationElement.NextPage.nextKeys] as
	 * [filterRecipients], and [MultiKeyPaginationElement.NextPage.nextDocId] as [startDocumentId]. All results have
	 * been returned once the flow completes without ever emitting a [MultiKeyPaginationElement.NextPage].
	 * @throws IllegalArgumentException when the returned flow is collected, if [filterRecipients] is empty or contains
	 * duplicates.
	 */
	fun findExchangeDataByDelegatorDelegateForRecipients(
		delegatorId: String,
		delegateId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
	): Flow<MultiKeyPaginationElement<ExchangeData, String?>>

	/**
	 * Get all pieces of exchange data that share [exchangeDataOrGroupId] as their [ExchangeData.exchangeDataGroupId],
	 * or the single exchange data with [exchangeDataOrGroupId] as its [ExchangeData.id] if there is no exchange data
	 * with that [ExchangeData.exchangeDataGroupId]. In the latter, fallback case the returned exchange data always has
	 * a null [ExchangeData.recipient], since [ExchangeData.recipient] is null exactly for the exchange data with no
	 * [ExchangeData.exchangeDataGroupId].
	 *
	 * Unlike [findExchangeDataGroupByIdForRecipients] this is not limited by recipient: it always returns every piece
	 * of the group (or the single, non-group exchange data).
	 *
	 * Note that [PaginationOffset.limit] is only an upper bound requested by the caller: it is capped to an internal
	 * maximum page size, so a page may hold fewer entities than requested even when more are available. Rely on the
	 * next page element to know whether there is more, not on the number of entities in a page.
	 * @param paginationOffset data for the paged retrieval of data.
	 * @return the events resulting from the DB interrogation.
	 */
	fun findExchangeDataGroupById(
		exchangeDataOrGroupId: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement>

	/**
	 * Create pieces of an exchange data group, for each of the provided recipients.
	 * Note that there is no validation on the [delegate] actually being a data owner group or on the recipients being
	 * actually members of the [delegate] group.
	 * @throws IllegalArgumentException if [piecesByRecipient] is empty.
	 */
	fun createExchangeDataGroupPieces(
		exchangeDataGroupId: String,
		delegator: String,
		delegate: String,
		piecesByRecipient: Map<String, ExchangeDataPieceCreationRequest>
	): Flow<ExchangeData>
}
