package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.EmptyObjectKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.couchdb.queryViewNoValue
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
import org.taktik.icure.entities.requests.ExchangeDataCounterpart
import org.taktik.icure.utils.multiKeyPaginatedViewQuery
import kotlin.collections.map

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
		valueType = String::class.java,
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
		valueType = Nothing::class.java,
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
		valueType = Nothing::class.java,
	) { recipient -> ComplexKey.of(exchangeDataOrGroupId, recipient) }

	@View(
		name = "by_participant_non_piece_counterpart",
		map = "classpath:js/exchangedata/By_participant_non_piece_counterpart_map.js",
		reduce = "classpath:js/exchangedata/By_participant_non_piece_counterpart_reduce.js",
		secondaryPartition = MAURICE_PARTITION,
	)
	override fun findNonGroupPieceCounterparts(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		startCounterpartId: String?,
		limit: Int,
	): Flow<ExchangeDataCounterpart> = flow {
		require(limit > 0) { "The limit should be positive." }
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(
			datastoreInformation = datastoreInformation,
			viewName = "by_participant_non_piece_counterpart",
			secondaryPartition = MAURICE_PARTITION,
		).includeDocs(false)
			/*
			 * by_participant_non_piece_counterpart is keyed by [participant, counterpartGroupId, counterpartId] and
			 * grouped on the whole key, so each row of the result is a distinct counterpart however many exchange data
			 * there is with it, and its value is the intersection of the usable keypair fingerprints of all of them.
			 * That is what makes a page of `limit` rows exactly `limit` counterparts, and it is also why the rows carry
			 * no document id: a reduced row has none, so the cursor can only be the key.
			 */
			.reduce(true)
			.group(true)
			.groupLevel(3)
			// A null group id is the counterparts of the group of the participant, and sorts before every other.
			.startKey(ComplexKey.of(dataOwnerId, null, startCounterpartId))
			.endKey(ComplexKey.of(dataOwnerId, null, EmptyObjectKey))
			.limit(limit)
			.descending(false)
		emitAll(
			client.queryView<ComplexKey, List<String>>(viewQuery).map { row ->
				ExchangeDataCounterpart(
					counterpartId = checkNotNull(row.key?.components?.get(2) as String?) {
						"The rows of by_participant_non_piece_counterpart always have a counterpart in their key."
					},
					usableKeypairFingerprints = row.value.orEmpty().toSet(),
				)
			},
		)
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when (partition) {
			// Every view of the Maurice partition lives in the same design document, and couchdb builds a design
			// document's views together, so warming one of them warms them all.
			Partitions.Maurice -> warmup(datastoreInformation, "by_participant_recipient" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}

	/**
	 * Queries [viewName] for one key per entry of [filterRecipients], as built by [keyForRecipient], returning at most
	 * [limit] entities, resuming from [startDocumentId] for the first recipient. See [multiKeyPaginatedViewQuery] for
	 * the mechanics, including why resuming costs a second query.
	 */
	private fun findExchangeDataForRecipients(
		datastoreInformation: IDatastoreInformation,
		viewName: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
		limit: Int,
		valueType: Class<*>,
		keyForRecipient: (recipient: String?) -> ComplexKey,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			multiKeyPaginatedViewQuery(
				keys = filterRecipients.map(keyForRecipient),
				keysDescription = "recipients used to filter the exchange data",
				startDocumentId = startDocumentId,
				limit = limit,
				viewQuery = {
					createQuery(
						datastoreInformation = datastoreInformation,
						viewName = viewName,
						secondaryPartition = MAURICE_PARTITION,
					).includeDocs(true)
						.reduce(false)
						.descending(false)
				},
			) { query ->
				client.queryView(query, ComplexKey::class.java, valueType, ExchangeData::class.java)
			},
		)
	}

	@View(
		name = "main_id_by_participant",
		map = "classpath:js/exchangedata/Main_id_by_participant_map.js",
		secondaryPartition = MAURICE_PARTITION,
	)
	override fun findMainExchangeDataIdsByParticipant(
		datastoreInformation: IDatastoreInformation,
		participantId: String,
		startDocumentId: String?,
		limit: Int,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewNoValue<String>(
				createQuery(
					datastoreInformation = datastoreInformation,
					viewName = "main_id_by_participant",
					secondaryPartition = MAURICE_PARTITION,
				).includeDocs(false)
					.key(participantId)
					.startDocId(startDocumentId)
					.limit(limit)
					.descending(false)
			).map { it.id }
		)
	}

	override fun findMainExchangeDataIdsByParticipantForRecipients(
		datastoreInformation: IDatastoreInformation,
		participantId: String,
		filterRecipients: List<String?>,
		startDocumentId: String?,
		limit: Int,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			multiKeyPaginatedViewQuery(
				keys = filterRecipients.map { recipient -> ComplexKey.of(participantId, recipient) },
				keysDescription = "recipients used to filter the exchange data",
				startDocumentId = startDocumentId,
				limit = limit,
				viewQuery = {
					createQuery(
						datastoreInformation = datastoreInformation,
						viewName = "by_participant_recipient",
						secondaryPartition = MAURICE_PARTITION,
					).includeDocs(false)
						.reduce(false)
						.descending(false)
				},
			) { query ->
				client.queryView(query, ComplexKey::class.java, String::class.java, Nothing::class.java)
			},
		)
	}
}
