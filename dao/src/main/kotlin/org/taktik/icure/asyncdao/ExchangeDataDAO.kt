package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.entities.requests.ExchangeDataCounterpart

interface ExchangeDataDAO : GenericDAO<ExchangeData> {
	/**
	 * Get all exchange data where the provided data owner is the delegator and/or delegate for the exchange data.
	 */
	fun findExchangeDataByParticipant(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		paginationOffset: PaginationOffset<String>,
	): Flow<ViewQueryResultEvent>

	/**
	 * Get all exchange data where [ExchangeData.delegator] is [delegatorId] and [ExchangeData.delegate] is
	 * [delegateId]. Note that the exchange data in the opposite direction, i.e. from [delegateId] to [delegatorId], is
	 * not returned.
	 */
	fun findExchangeDataByDelegatorDelegatePair(
		datastoreInformation: IDatastoreInformation,
		delegatorId: String,
		delegateId: String,
	): Flow<ExchangeData>

	/**
	 * Get all exchange data where the provided data owner is the delegator and/or delegate for the exchange data,
	 * limited to the exchange data whose [ExchangeData.recipient] is in [filterRecipients]. [ExchangeData.recipient]
	 * is null exactly for the exchange data with no [ExchangeData.exchangeDataGroupId] (a plain exchange data, or the
	 * exchange data of a parent-type group): include `null` as an entry of [filterRecipients] to also get that
	 * exchange data back.
	 *
	 * # Pagination
	 * This is a "by_keys" request on the keys [dataOwnerId] x [filterRecipients] and returns at most [limit] entities.
	 * A request asking for many recipients may be truncated even if each single recipient has only few exchange data.
	 *
	 * The rows of different recipients are never interleaved, and [startDocumentId] constrains only the rows of the
	 * first entry of [filterRecipients]. When asking for the next page you must therefore:
	 * - pass the id of the last entity of the previous page as [startDocumentId];
	 * - keep the recipient of that last entity as the first entry of [filterRecipients], since it is the only recipient
	 *   that may have been partially returned;
	 * - omit all the other recipients already returned by the previous pages, since their exchange data is complete.
	 *
	 * This is boundary-inclusive: the entity with id [startDocumentId] is returned again as the first entity of the
	 * page. To browse without ever processing the same entity twice, request one more than the page size you want
	 * and, whenever a page holds that many entities, use only its last entity to build the next [startDocumentId] and
	 * [filterRecipients] cursor without treating it as page content: it will reappear as the first entity of the next
	 * page.
	 *
	 * All results have been returned once a page holds fewer than [limit] entities.
	 * @throws IllegalArgumentException when the returned flow is collected, if [filterRecipients] is empty or contains
	 * duplicates, or if [limit] is not positive.
	 */
	fun findExchangeDataByParticipantForRecipients(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
		limit: Int,
	): Flow<ViewQueryResultEvent>

	/**
	 * Get all exchange data where [ExchangeData.delegator] is [delegatorId] and [ExchangeData.delegate] is [delegateId],
	 * limited to the exchange data whose [ExchangeData.recipient] is in [filterRecipients]. [ExchangeData.recipient]
	 * is null exactly for the exchange data with no [ExchangeData.exchangeDataGroupId] (a plain exchange data, or the
	 * exchange data of a parent-type group): include `null` as an entry of [filterRecipients] to also get that
	 * exchange data back.
	 *
	 * Note that, unlike [findExchangeDataByDelegatorDelegatePair], this method is paginated and returns the raw view
	 * results instead of the entities.
	 *
	 * # Pagination
	 * This is a "by_keys" request on the keys ([delegatorId], [delegateId]) x [filterRecipients] and returns at most
	 * [limit] entities. A request asking for many recipients may be truncated even if each single recipient has only
	 * few exchange data.
	 *
	 * The rows of different recipients are never interleaved, and [startDocumentId] constrains only the rows of the
	 * first entry of [filterRecipients]. When asking for the next page you must therefore:
	 * - pass the id of the last entity of the previous page as [startDocumentId];
	 * - keep the recipient of that last entity as the first entry of [filterRecipients], since it is the only recipient
	 *   that may have been partially returned;
	 * - omit all the other recipients already returned by the previous pages, since their exchange data is complete.
	 *
	 * This is boundary-inclusive: the entity with id [startDocumentId] is returned again as the first entity of the
	 * page. To browse without ever processing the same entity twice, request one more than the page size you want
	 * and, whenever a page holds that many entities, use only its last entity to build the next [startDocumentId] and
	 * [filterRecipients] cursor without treating it as page content: it will reappear as the first entity of the next
	 * page.
	 *
	 * All results have been returned once a page holds fewer than [limit] entities.
	 * @throws IllegalArgumentException when the returned flow is collected, if [filterRecipients] is empty or contains
	 * duplicates, or if [limit] is not positive.
	 */
	fun findExchangeDataByDelegatorDelegateForRecipients(
		datastoreInformation: IDatastoreInformation,
		delegatorId: String,
		delegateId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
		limit: Int,
	): Flow<ViewQueryResultEvent>

	/**
	 * Get all pieces of exchange data that share [exchangeDataOrGroupId] as their [ExchangeData.exchangeDataGroupId],
	 * or the single exchange data with [exchangeDataOrGroupId] as its [ExchangeData.id] if there is no exchange data
	 * with that [ExchangeData.exchangeDataGroupId]. In the latter, fallback case the returned exchange data always has
	 * a null [ExchangeData.recipient], since [ExchangeData.recipient] is null exactly for the exchange data with no
	 * [ExchangeData.exchangeDataGroupId].
	 *
	 * # Pagination
	 * This is a range query on the keys from [exchangeDataOrGroupId] alone up to [exchangeDataOrGroupId] paired with
	 * any recipient, with a caller-provided page size ([PaginationOffset.limit]).
	 *
	 * To ask for the first page leave [PaginationOffset.startKey] and [PaginationOffset.startDocumentId] null.
	 *
	 * This is boundary-inclusive: the entity whose key and id are [PaginationOffset.startKey] and
	 * [PaginationOffset.startDocumentId] is returned again as the first entity of the page. To browse without ever
	 * processing the same entity twice, request one more than the page size you want and, whenever a page holds that
	 * many entities, use only its last entity to build the next [PaginationOffset.startKey] and
	 * [PaginationOffset.startDocumentId] without treating it as page content: it will reappear as the first entity of
	 * the next page.
	 *
	 * All results have been returned once a page holds fewer entities than [PaginationOffset.limit].
	 * @throws IllegalArgumentException when the returned flow is collected, if [PaginationOffset.startKey] is not null
	 * and is not a key of [exchangeDataOrGroupId].
	 */
	fun findExchangeDataGroupById(
		datastoreInformation: IDatastoreInformation,
		exchangeDataOrGroupId: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<ViewQueryResultEvent>

	/**
	 * Get all pieces of exchange data that share [exchangeDataOrGroupId] as their [ExchangeData.exchangeDataGroupId],
	 * or the single exchange data with [exchangeDataOrGroupId] as its [ExchangeData.id] if there is no exchange data
	 * with that [ExchangeData.exchangeDataGroupId], limited to the exchange data whose [ExchangeData.recipient] is in
	 * [filterRecipients]. [ExchangeData.recipient] is null exactly for the exchange data with no
	 * [ExchangeData.exchangeDataGroupId] (a plain exchange data, or the exchange data of a parent-type group): a
	 * request for [exchangeDataOrGroupId] that falls back to a plain exchange data can therefore only ever match it if
	 * `null` is an entry of [filterRecipients].
	 *
	 * # Pagination
	 * This is a "by_keys" request on the keys [exchangeDataOrGroupId] x [filterRecipients] and returns at most [limit]
	 * entities. A request asking for many recipients may be truncated even if each single recipient has only few
	 * exchange data.
	 *
	 * The rows of different recipients are never interleaved, and [startDocumentId] constrains only the rows of the
	 * first entry of [filterRecipients]. When asking for the next page you must therefore:
	 * - pass the id of the last entity of the previous page as [startDocumentId];
	 * - keep the recipient of that last entity as the first entry of [filterRecipients], since it is the only recipient
	 *   that may have been partially returned;
	 * - omit all the other recipients already returned by the previous pages, since their exchange data is complete.
	 *
	 * This is boundary-inclusive: the entity with id [startDocumentId] is returned again as the first entity of the
	 * page. To browse without ever processing the same entity twice, request one more than the page size you want
	 * and, whenever a page holds that many entities, use only its last entity to build the next [startDocumentId] and
	 * [filterRecipients] cursor without treating it as page content: it will reappear as the first entity of the next
	 * page.
	 *
	 * All results have been returned once a page holds fewer than [limit] entities.
	 * @throws IllegalArgumentException when the returned flow is collected, if [filterRecipients] is empty or contains
	 * duplicates, or if [limit] is not positive.
	 */
	fun findExchangeDataGroupByIdForRecipients(
		datastoreInformation: IDatastoreInformation,
		exchangeDataOrGroupId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
		limit: Int,
	): Flow<ViewQueryResultEvent>

	/**
	 * Get the distinct data owners that [dataOwnerId] shares exchange data with, that is the [ExchangeData.delegate] of
	 * the exchange data where [dataOwnerId] is the [ExchangeData.delegator] and the other way around, along with the
	 * keypairs that exchange data is usable with ([ExchangeDataCounterpart.usableKeypairFingerprints]).
	 *
	 * Only the exchange data with a null [ExchangeData.recipient] is considered: the pieces of a simple-type data owner
	 * group are created and re-encrypted through the group pieces flow, so a simple-type group is never returned as a
	 * counterpart by this search. [dataOwnerId] itself is never returned either.
	 *
	 * Only the counterparts belonging to the same group as [dataOwnerId] are returned: a counterpart referenced as
	 * "dataOwnerGroupId/dataOwnerId" is indexed but skipped. [dataOwnerId] itself may be such a reference.
	 *
	 * # Pagination
	 * Each returned [ExchangeDataCounterpart] is a distinct counterpart, however many exchange data there is with it,
	 * so a page of [limit] rows holds exactly [limit] counterparts.
	 *
	 * To ask for the first page leave [startCounterpartId] null.
	 *
	 * This is boundary-inclusive, like the other searches of this dao: the counterpart named by [startCounterpartId] is
	 * returned again as the first row of the page. To browse without ever processing the same counterpart twice,
	 * request one more than the page size you want and, whenever a page holds that many rows, use only its last row to
	 * build the next [startCounterpartId] without treating it as page content: it will reappear as the first row of
	 * the next page.
	 *
	 * All results have been returned once a page holds fewer than [limit] rows.
	 * @throws IllegalArgumentException when the returned flow is collected, if [limit] is not positive.
	 */
	fun findNonGroupPieceCounterparts(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		startCounterpartId: String?,
		limit: Int,
	): Flow<ExchangeDataCounterpart>
}
