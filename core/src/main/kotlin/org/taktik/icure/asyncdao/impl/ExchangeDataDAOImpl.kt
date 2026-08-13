package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.EmptyObjectKey
import org.taktik.couchdb.entity.ViewQuery
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.ExchangeDataDAO
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ExchangeData

@Repository("ExchangeDataDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.ExchangeData' && !doc.deleted) emit( null, doc._id )}",
)
class ExchangeDataDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider,
) : GenericDAOImpl<ExchangeData>(
	entityClass = ExchangeData::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider,
), ExchangeDataDAO {
	@View(name = "by_participant", map = "classpath:js/exchangedata/By_participant_map.js")
	override fun findExchangeDataByParticipant(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		paginationOffset: PaginationOffset<String>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		require(paginationOffset.startKey == null || paginationOffset.startKey == dataOwnerId) {
			"Pagination key should be the same as the data owner id if present."
		}
		val viewQuery =
			createQuery(datastoreInformation, "by_participant")
				.key(dataOwnerId)
				.includeDocs(true)
				.reduce(false)
				.startDocId(paginationOffset.startDocumentId)
				.limit(paginationOffset.limit)
				.descending(false)
		emitAll(client.queryView(viewQuery, String::class.java, Nothing::class.java, ExchangeData::class.java))
	}

	@View(name = "by_delegator_delegate", map = "classpath:js/exchangedata/By_delegator_delegate_map.js")
	override fun findExchangeDataByDelegatorDelegatePair(
		datastoreInformation: IDatastoreInformation,
		delegatorId: String,
		delegateId: String,
	): Flow<ExchangeData> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery =
			createQuery(datastoreInformation, "by_delegator_delegate")
				.key(ComplexKey.of(delegatorId, delegateId))
				.includeDocs(true)
				.reduce(false)
				.descending(false)
		client.queryViewIncludeDocsNoValue<ComplexKey, ExchangeData>(viewQuery).collect {
			emit(it.doc)
		}
	}

	@View(
		name = "by_participant_recipient",
		map = "classpath:js/exchangedata/By_participant_recipient_map.js",
		secondaryPartition = MAURICE_PARTITION,
	)
	override fun findExchangeDataByParticipantForRecipients(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
		limit: Int,
	): Flow<ViewQueryResultEvent> = findExchangeDataForRecipients(
		datastoreInformation = datastoreInformation,
		viewName = "by_participant_recipient",
		filterRecipients = filterRecipients,
		startDocumentId = startDocumentId,
		limit = limit,
	) { recipient -> ComplexKey.of(dataOwnerId, recipient) }

	@View(
		name = "by_delegator_delegate_recipient",
		map = "classpath:js/exchangedata/By_delegator_delegate_recipient_map.js",
		secondaryPartition = MAURICE_PARTITION,
	)
	override fun findExchangeDataByDelegatorDelegateForRecipients(
		datastoreInformation: IDatastoreInformation,
		delegatorId: String,
		delegateId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
		limit: Int,
	): Flow<ViewQueryResultEvent> = findExchangeDataForRecipients(
		datastoreInformation = datastoreInformation,
		viewName = "by_delegator_delegate_recipient",
		filterRecipients = filterRecipients,
		startDocumentId = startDocumentId,
		limit = limit,
	) { recipient -> ComplexKey.of(delegatorId, delegateId, recipient) }

	@View(
		name = "by_exchange_data_group_id_recipient",
		map = "classpath:js/exchangedata/By_exchange_data_group_id_recipient_map.js",
		secondaryPartition = MAURICE_PARTITION,
	)
	override fun findExchangeDataGroupById(
		datastoreInformation: IDatastoreInformation,
		exchangeDataOrGroupId: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<ViewQueryResultEvent> = flow {
		require(paginationOffset.startKey?.components?.firstOrNull().let { it == null || it == exchangeDataOrGroupId }) {
			"Pagination key should be a key of the exchange data or group id if present."
		}
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(
			datastoreInformation = datastoreInformation,
			viewName = "by_exchange_data_group_id_recipient",
			startKey = ComplexKey.of(exchangeDataOrGroupId, null),
			endKey = ComplexKey.of(exchangeDataOrGroupId, EmptyObjectKey),
			pagination = paginationOffset,
			descending = false,
			secondaryPartition = MAURICE_PARTITION,
		)
		emitAll(client.queryView(viewQuery, ComplexKey::class.java, Nothing::class.java, ExchangeData::class.java))
	}

	override fun findExchangeDataGroupByIdForRecipients(
		datastoreInformation: IDatastoreInformation,
		exchangeDataOrGroupId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
		limit: Int,
	): Flow<ViewQueryResultEvent> = findExchangeDataForRecipients(
		datastoreInformation = datastoreInformation,
		viewName = "by_exchange_data_group_id_recipient",
		filterRecipients = filterRecipients,
		startDocumentId = startDocumentId,
		limit = limit,
	) { recipient -> ComplexKey.of(exchangeDataOrGroupId, recipient) }

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when (partition) {
			Partitions.Maurice -> warmup(datastoreInformation, "by_participant_recipient" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}

	/**
	 * Queries [viewName] for one key per entry of [filterRecipients], as built by [keyForRecipient], returning at most
	 * [limit] entities.
	 * [startDocumentId] is applied to the first entry of [filterRecipients] only, since it is the only recipient that
	 * may have been partially returned by the previous page.
	 */
	private fun findExchangeDataForRecipients(
		datastoreInformation: IDatastoreInformation,
		viewName: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
		limit: Int,
		keyForRecipient: (recipient: String?) -> ComplexKey,
	): Flow<ViewQueryResultEvent> = flow {
		require(filterRecipients.isNotEmpty()) {
			"At least one recipient should be provided to filter the exchange data."
		}
		require(filterRecipients.distinct().size == filterRecipients.size) {
			"The recipients used to filter the exchange data should not contain duplicates."
		}
		require(limit > 0) {
			"The limit should be positive."
		}
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val keys = filterRecipients.map(keyForRecipient)

		suspend fun query(
			datastoreInformation: IDatastoreInformation,
			viewName: String,
		): ViewQuery = createQuery(
			datastoreInformation = datastoreInformation,
			viewName = viewName,
			secondaryPartition = MAURICE_PARTITION,
		).includeDocs(true)
			.reduce(false)
			.descending(false)

		if (startDocumentId == null || keys.size == 1) {
			emitAll(
				client.queryView(
					query(datastoreInformation, viewName)
						.keys(keys)
						.startDocId(startDocumentId)
						.limit(limit),
					ComplexKey::class.java,
					Nothing::class.java,
					ExchangeData::class.java,
				),
			)
		} else {
			// The start document id is only applied to the rows of a single key, so the first recipient has to be
			// queried separately from the others.
			val returnedForFirstRecipient = client.queryView(
				query(datastoreInformation, viewName)
					.key(keys.first())
					.startDocId(startDocumentId)
					.limit(limit),
				ComplexKey::class.java,
				Nothing::class.java,
				ExchangeData::class.java,
			).onEach { emit(it) }.count { it is ViewRowWithDoc<*, *, *> }
			if (returnedForFirstRecipient < limit) {
				emitAll(
					client.queryView(
						query(datastoreInformation, viewName)
							.keys(keys.drop(1))
							.limit(limit - returnedForFirstRecipient),
						ComplexKey::class.java,
						Nothing::class.java,
						ExchangeData::class.java,
					),
				)
			}
		}
	}
}
