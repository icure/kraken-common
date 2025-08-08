/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Message
import org.taktik.icure.utils.distinctById
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.main

// Differences between lite and cloud version: instantiated as a bean in the respective DAOConfig
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Message' && !doc.deleted) emit( null, doc._id )}")
open class MessageDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
) : GenericIcureDAOImpl<Message>(Message::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig),
	MessageDAO {

	companion object {
		private const val SMALLEST_CHAR = "\u0000"
	}

	@Views(
		View(name = "by_hcparty_from_address", map = "classpath:js/message/By_hcparty_from_address_map.js"),
		View(
			name = "by_data_owner_from_address",
			map = "classpath:js/message/By_data_owner_from_address_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listMessagesByFromAddress(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		fromAddress: String,
		paginationOffset: PaginationOffset<ComplexKey>,
		reverse: Boolean,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(partyId, fromAddress, if (reverse) ComplexKey.emptyObject() else null)
		val endKey = ComplexKey.of(partyId, fromAddress, if (reverse) null else ComplexKey.emptyObject())

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_from_address",
			"by_data_owner_from_address" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			paginationOffset,
			reverse,
		)
		emitAll(
			client.interleave<ComplexKey, String, Message>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ (it.components[2] as? Number)?.toLong() },
				),
			),
		)
	}

	override fun listMessageIdsByFromAddress(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		fromAddress: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(dataOwnerId, fromAddress, null)
		val endKey = ComplexKey.of(dataOwnerId, fromAddress, ComplexKey.emptyObject())

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_from_address",
			"by_data_owner_from_address" to DATA_OWNER_PARTITION,
		).startKey(startKey)
			.endKey(endKey)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ (it.components[2] as? Number)?.toLong() },
				),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	@Views(
		View(name = "by_hcparty_to_address", map = "classpath:js/message/By_hcparty_to_address_map.js"),
		View(
			name = "by_data_owner_to_address",
			map = "classpath:js/message/By_data_owner_to_address_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun findMessagesByToAddress(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		toAddress: String,
		paginationOffset: PaginationOffset<ComplexKey>,
		reverse: Boolean,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(partyId, toAddress, if (reverse) ComplexKey.emptyObject() else null)
		val endKey = ComplexKey.of(partyId, toAddress, if (reverse) null else ComplexKey.emptyObject())

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_to_address",
			"by_data_owner_to_address" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			paginationOffset,
			reverse,
		)
		emitAll(
			client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }, { (it.components[2] as? Number)?.toLong() })),
		)
	}

	override fun listMessageIdsByToAddress(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		toAddress: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(dataOwnerId, toAddress, null)
		val endKey = ComplexKey.of(dataOwnerId, toAddress, ComplexKey.emptyObject())

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_to_address",
			"by_data_owner_to_address" to DATA_OWNER_PARTITION,
		).startKey(startKey)
			.endKey(endKey)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ (it.components[2] as? Number)?.toLong() },
				),
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().map { it.id },
		)
	}

	private fun getGuidPrefix(transportGuid: String?): String? = transportGuid?.takeIf { it.endsWith(":*") }?.substring(0, transportGuid.length - 1)

	@Views(
		View(name = "by_hcparty_transport_guid", map = "classpath:js/message/By_hcparty_transport_guid_map.js"),
		View(name = "by_data_owner_transport_guid", map = "classpath:js/message/By_data_owner_transport_guid_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findMessagesByTransportGuid(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		transportGuid: String?,
		paginationOffset: PaginationOffset<ComplexKey>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = getGuidPrefix(transportGuid)?.let { ComplexKey.of(partyId, it) } ?: ComplexKey.of(partyId, transportGuid)
		val endKey = getGuidPrefix(transportGuid)?.let { ComplexKey.of(partyId, it + "\ufff0") } ?: ComplexKey.of(partyId, transportGuid)

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_transport_guid",
			"by_data_owner_transport_guid" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			paginationOffset,
			false,
		)
		emitAll(client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String })))
	}

	@Views(
		View(name = "by_hcparty_transport_guid_received", map = "classpath:js/message/By_hcparty_transport_guid_received_map.js"),
		View(name = "by_data_owner_transport_guid_received", map = "classpath:js/message/By_data_owner_transport_guid_received_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findMessagesByTransportGuidReceived(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		transportGuid: String?,
		paginationOffset: PaginationOffset<ComplexKey>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = getGuidPrefix(transportGuid)?.let { ComplexKey.of(partyId, it, null) } ?: ComplexKey.of(partyId, transportGuid, null)
		val endKey = getGuidPrefix(transportGuid)?.let { ComplexKey.of(partyId, it + "\ufff0", ComplexKey.emptyObject()) } ?: ComplexKey.of(partyId, transportGuid, ComplexKey.emptyObject())

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_transport_guid_received",
			"by_data_owner_transport_guid_received" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			paginationOffset,
			false,
		)
		emitAll(client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }, { (it.components[2] as? Number)?.toLong() })))
	}

	override fun listMessageIdsByTransportGuidReceived(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		transportGuid: String,
		descending: Boolean,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = getGuidPrefix(transportGuid)?.let { ComplexKey.of(dataOwnerId, it, null) } ?: ComplexKey.of(dataOwnerId, transportGuid, null)
		val endKey = getGuidPrefix(transportGuid)?.let { ComplexKey.of(dataOwnerId, it + "\ufff0", ComplexKey.emptyObject()) } ?: ComplexKey.of(dataOwnerId, transportGuid, ComplexKey.emptyObject())

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_transport_guid_received",
			"by_data_owner_transport_guid_received" to DATA_OWNER_PARTITION,
		).let {
			if (descending) {
				it.startKey(endKey).endKey(startKey)
			} else {
				it.startKey(startKey).endKey(endKey)
			}
		}.descending(descending).doNotIncludeDocs()

		emitAll(
			client
				.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }, { (it.components[2] as? Number)?.toLong() }))
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.map { it.id },
		)
	}

	@Views(
		View(name = "by_hcparty_transport_guid_sent_date", map = "classpath:js/message/By_hcparty_transport_guid_sent_date.js"),
		View(name = "by_data_owner_transport_guid_sent_date", map = "classpath:js/message/By_data_owner_transport_guid_sent_date.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findMessagesByTransportGuidAndSentDate(datastoreInformation: IDatastoreInformation, partyId: String, transportGuid: String, fromDate: Long, toDate: Long, paginationOffset: PaginationOffset<ComplexKey>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val startKey = ComplexKey.of(partyId, transportGuid, fromDate)
		val endKey = ComplexKey.of(partyId, transportGuid, toDate)

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_transport_guid_sent_date",
			"by_data_owner_transport_guid_sent_date" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			paginationOffset,
			false,
		)
		emitAll(client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }, { (it.components[2] as? Number)?.toLong() })))
	}

	override fun listMessageIdsByTransportGuidAndSentDate(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		transportGuid: String,
		fromDate: Long?,
		toDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val startKey = ComplexKey.of(dataOwnerId, transportGuid + SMALLEST_CHAR, fromDate)
		val endKey = ComplexKey.of(dataOwnerId, transportGuid, toDate ?: ComplexKey.emptyObject())

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_transport_guid_sent_date",
			"by_data_owner_transport_guid_sent_date" to DATA_OWNER_PARTITION,
		).let {
			if (descending) {
				it.startKey(endKey).endKey(startKey)
			} else {
				it.startKey(startKey).endKey(endKey)
			}
		}.descending(descending)

		emitAll(
			client
				.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }, { (it.components[2] as? Number)?.toLong() }))
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.map { it.id },
		)
	}

	@Views(
		View(name = "by_hcparty", map = "classpath:js/message/By_hcParty_map.js"),
		View(name = "by_data_owner", map = "classpath:js/message/By_dataOwner_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findMessagesByHcPartySortedByReceived(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		paginationOffset: PaginationOffset<ComplexKey>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val startKey: ComplexKey = ComplexKey.of(partyId, null)
		val endKey: ComplexKey = ComplexKey.of(partyId, ComplexKey.emptyObject())
		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty",
			"by_data_owner" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			paginationOffset,
			false,
		).reduce(false)
		emitAll(
			client
				.interleave<ComplexKey, String?, Message>(
					viewQueries,
					compareBy({ it.components[0] as? String }, { (it.components[1] as? Number)?.toLong() }),
				),
		)
	}

	@Deprecated("This method is inefficient for high volumes of keys, use listMessageIdsByDataOwnerPatientSentDate instead")
	@Views(
		View(name = "by_hcparty_patientfk", map = "classpath:js/message/By_hcparty_patientfk_map.js"),
		View(name = "by_data_owner_patientfk", map = "classpath:js/message/By_data_owner_patientfk_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listMessagesByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { ComplexKey.of(it, fk) }
		}
		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patientfk".main(),
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION,
		).keys(keys).includeDocs()
		emitAll(
			client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({ it.components[0] as String }, { it.components[1] as String }))
				.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Message>>().map { it.doc }.distinctUntilChangedBy { it.id },
		)
	}.distinctById()

	@View(name = "by_hcparty_patientfk_sent", map = "classpath:js/message/By_hcparty_patientfk_sent_map.js", secondaryPartition = MAURICE_PARTITION)
	override fun listMessageIdsByDataOwnerPatientSentDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = getEntityIdsByDataOwnerPatientDate(
		views = listOf("by_hcparty_patientfk_sent" to MAURICE_PARTITION, "by_data_owner_patientfk" to DATA_OWNER_PARTITION),
		datastoreInformation = datastoreInformation,
		searchKeys = searchKeys,
		secretForeignKeys = secretForeignKeys,
		startDate = startDate,
		endDate = endDate,
		descending = descending,
	)

	@View(name = "by_parent_id", map = "classpath:js/message/By_parent_id_map.js")
	override fun getChildren(datastoreInformation: IDatastoreInformation, messageId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocs<String, Int, Message>(createQuery(datastoreInformation, "by_parent_id").includeDocs(true).key(messageId)).map { it.doc })
	}

	override fun getMessagesChildren(datastoreInformation: IDatastoreInformation, parentIds: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val query = createQuery(datastoreInformation, "by_parent_id").includeDocs(true).keys(parentIds)
		emitAll(client.queryViewIncludeDocs<String, Int, Message>(query).map { it.doc })
	}

	override fun listMessageIdsByParents(datastoreInformation: IDatastoreInformation, parentIds: List<String>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val query = createQuery(datastoreInformation, "by_parent_id").includeDocs(false).keys(parentIds)
		emitAll(client.queryView<String, Int>(query).map { it.id })
	}

	override fun findMessagesByIds(
		datastoreInformation: IDatastoreInformation,
		messageIds: Collection<String>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(messageIds, Message::class.java))
	}

	@View(name = "by_invoice_id", map = "classpath:js/message/By_invoice_id_map.js")
	override fun listMessagesByInvoiceIds(datastoreInformation: IDatastoreInformation, invoiceIds: Set<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocs<String, Int, Message>(createQuery(datastoreInformation, "by_invoice_id").includeDocs(true).keys(invoiceIds)).map { it.doc })
	}

	override fun listMessageIdsByInvoiceIds(datastoreInformation: IDatastoreInformation, invoiceIds: Set<String>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView<String, Int>(createQuery(datastoreInformation, "by_invoice_id").includeDocs(false).keys(invoiceIds)).map { it.id })
	}

	@Views(
		View(name = "by_hcparty_transport_guid", map = "classpath:js/message/By_hcparty_transport_guid_map.js"),
		View(name = "by_data_owner_transport_guid", map = "classpath:js/message/By_data_owner_transport_guid_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun getMessagesByTransportGuids(datastoreInformation: IDatastoreInformation, hcPartyId: String, transportGuids: Collection<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_transport_guid",
			"by_data_owner_transport_guid" to DATA_OWNER_PARTITION,
		).keys(HashSet(transportGuids).map { ComplexKey.of(hcPartyId, it) }).includeDocs()
		emitAll(
			client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({ it.components[0] as String }, { it.components[1] as String }))
				.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Message>>().map { it.doc }.distinctUntilChangedBy { it.id },
		)
	}

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Message' && !doc.deleted && doc._conflicts) emit(doc._id )}")
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocsNoValue<String, Message>(createQuery(datastoreInformation, "conflicts").includeDocs(true)).map { it.doc })
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when (partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_transport_guid" to DATA_OWNER_PARTITION)
			Partitions.Maurice -> warmup(datastoreInformation, "by_hcparty_patientfk_sent" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}

	@View(name = "by_data_owner_and_last_update", map = "classpath:js/message/By_data_owner_and_last_update.js", secondaryPartition = DATA_OWNER_PARTITION)
	override fun listMessageIdsByDataOwnerLifecycleBetween(
		datastoreInformation: IDatastoreInformation,
		searchKey: String,
		startTimestamp: Long?,
		endTimestamp: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from = if (descending) ComplexKey.of(searchKey, endTimestamp ?: ComplexKey.emptyObject()) else ComplexKey.of(searchKey, startTimestamp)
		val to = if (descending) ComplexKey.of(searchKey, startTimestamp) else ComplexKey.of(searchKey, endTimestamp ?: ComplexKey.emptyObject())
		val query = createQuery(
			datastoreInformation,
			"by_data_owner_and_last_update",
			DATA_OWNER_PARTITION,
		).startKey(from).endKey(to).descending(descending).includeDocs(false)
		emitAll(
			client.queryView<ComplexKey, String>(query)
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.map { it.id },
		)
	}
}
