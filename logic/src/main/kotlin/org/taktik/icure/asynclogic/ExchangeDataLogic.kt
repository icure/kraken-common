package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.pagination.PaginationElement

interface ExchangeDataLogic {
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
	 * @param paginationOffset data for the paged retrevial of data.
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
	 * @return the created exchange data, with updated revision number, errors have been filtered out.
	 */
	fun createExchangeDatas(exchangeDatas: List<ExchangeData>): Flow<ExchangeData>

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
	fun getParticipantCounterparts(
		dataOwnerId: String,
		counterpartsType: List<DataOwnerType>,
		ignoreOnEntryForFingerprint: String?,
	): Flow<String>
}
