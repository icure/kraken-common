/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Message
import org.taktik.icure.utils.*

// Differences between lite and cloud version: instantiated as a bean in the respective DAOConfig
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Message' && !doc.deleted) emit( null, doc._id )}")
open class MessageDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider
) : GenericIcureDAOImpl<Message>(Message::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(Message::class.java), designDocumentProvider), MessageDAO {

	@Views(
		View(name = "by_hcparty_from_address_actor", map = "classpath:js/message/By_hcparty_from_address_actor_map.js"),
		View(
			name = "by_data_owner_from_address_actor",
			map = "classpath:js/message/By_data_owner_from_address_actor_map.js",
			secondaryPartition = DATA_OWNER_PARTITION
		),
	)
	override fun listMessagesByFromAddressAndActor(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		fromAddress: String,
		actorKeys: List<String>?
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		actorKeys?.takeIf { it.isNotEmpty() }?.let { it ->
			val viewQueries = createQueries(
				datastoreInformation,
				"by_hcparty_from_address_actor",
				"by_data_owner_from_address_actor" to DATA_OWNER_PARTITION
			).keys(it.map { k: String -> ComplexKey.of(partyId, fromAddress, k) }).includeDocs()

			emitAll(client.interleave<ComplexKey, String, Message>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ it.components[2] as? String })
			).filterIsInstance<ViewRowWithDoc<ComplexKey, String, Message>>().map { it.doc })
		}
	}

	@Views(
		View(name = "by_hcparty_to_address_actor", map = "classpath:js/message/By_hcparty_to_address_actor_map.js"),
		View(
			name = "by_data_owner_to_address_actor",
			map = "classpath:js/message/By_data_owner_to_address_actor_map.js",
			secondaryPartition = DATA_OWNER_PARTITION
		),
	)
	override fun listMessagesByToAddressAndActor(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		toAddress: String,
		actorKeys: List<String>?
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		actorKeys?.takeIf { it.isNotEmpty() }?.let { keys ->
			emitAll(client.interleave<ComplexKey, String, Message>(
				createQueries(
					datastoreInformation,
					"by_hcparty_to_address_actor",
					"by_data_owner_to_address_actor" to DATA_OWNER_PARTITION
				).keys(keys.map { ComplexKey.of(partyId, toAddress, it) }).includeDocs(),
				compareBy({ it.components[0] as? String },
					{ it.components[1] as? String },
					{ it.components[2] as? String })
			).filterIsInstance<ViewRowWithDoc<ComplexKey, String, Message>>().map { it.doc })
		}
	}

	@Views(
		View(
			name = "by_hcparty_transport_guid_actor",
			map = "classpath:js/message/By_hcparty_transport_guid_actor_map.js"
		),
		View(
			name = "by_data_owner_transport_guid_actor",
			map = "classpath:js/message/By_data_owner_transport_guid_actor_map.js",
			secondaryPartition = DATA_OWNER_PARTITION
		),
	)
	override fun listMessagesByTransportGuidAndActor(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		transportGuid: String,
		actorKeys: List<String>?
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		actorKeys?.takeIf { it.isNotEmpty() }?.let { keys ->
			emitAll(client.interleave<ComplexKey, String, Message>(
				createQueries(
					datastoreInformation,
					"by_hcparty_transport_guid_actor",
					"by_data_owner_transport_guid_actor" to DATA_OWNER_PARTITION
				).keys(keys.map { ComplexKey.of(partyId, transportGuid, it) }).includeDocs(),
				compareBy({ it.components[0] as? String },
					{ it.components[1] as? String },
					{ it.components[2] as? String })
			).filterIsInstance<ViewRowWithDoc<ComplexKey, String, Message>>().map { it.doc })
		}
	}

	@Views(
		View(name = "by_hcparty_from_address", map = "classpath:js/message/By_hcparty_from_address_map.js"),
		View(
			name = "by_data_owner_from_address",
			map = "classpath:js/message/By_data_owner_from_address_map.js",
			secondaryPartition = DATA_OWNER_PARTITION
		),
	)
	override fun listMessagesByFromAddress(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		fromAddress: String,
		paginationOffset: PaginationOffset<ComplexKey>,
		reverse: Boolean
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
			reverse
		)
		emitAll(
			client.interleave<ComplexKey, String, Message>(
				viewQueries,
				compareBy({ it.components[0] as? String },
					{ it.components[1] as? String },
					{ (it.components[2] as? Number)?.toLong() })
			)
		)
	}

	@Views(
		View(name = "by_hcparty_to_address", map = "classpath:js/message/By_hcparty_to_address_map.js"),
		View(
			name = "by_data_owner_to_address",
			map = "classpath:js/message/By_data_owner_to_address_map.js",
			secondaryPartition = DATA_OWNER_PARTITION
		),
	)
	override fun findMessagesByToAddress(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		toAddress: String,
		paginationOffset: PaginationOffset<ComplexKey>,
		reverse: Boolean
	) =
		flow {
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
				reverse
			)
			emitAll(
				client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }, { (it.components[2] as? Number)?.toLong() }))
			)
		}

	@Views(
    	View(name = "by_hcparty_transport_guid", map = "classpath:js/message/By_hcparty_transport_guid_map.js"),
    	View(name = "by_data_owner_transport_guid", map = "classpath:js/message/By_data_owner_transport_guid_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findMessagesByTransportGuid(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		transportGuid: String?,
		paginationOffset: PaginationOffset<ComplexKey>
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = transportGuid?.takeIf { it.endsWith(":*") }?.let {
			val prefix = transportGuid.substring(0, transportGuid.length - 1)
			ComplexKey.of(partyId, prefix)
		} ?: ComplexKey.of(partyId, transportGuid)

		val endKey = transportGuid?.takeIf { it.endsWith(":*") }?.let {
			val prefix = transportGuid.substring(0, transportGuid.length - 1)
			ComplexKey.of(partyId, prefix + "\ufff0")
		} ?: ComplexKey.of(partyId, transportGuid)

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_transport_guid",
			"by_data_owner_transport_guid" to DATA_OWNER_PARTITION,
			startKey, endKey, paginationOffset, false
		)
		emitAll(client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String})))
	}

	override fun listMessageIdsByTransportGuid(
		datastoreInformation: IDatastoreInformation,
		hcPartyId: String,
		transportGuid: String?
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(hcPartyId, transportGuid)
		val endKey = ComplexKey.of(hcPartyId, transportGuid)

		val viewQueries = createQueries(
			datastoreInformation, "by_hcparty_transport_guid", "by_data_owner_transport_guid" to DATA_OWNER_PARTITION
		)
			.startKey(startKey)
			.endKey(endKey)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as? String? }, { (it.components[1] as? Number?)?.toLong() }))
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.mapNotNull { it.id }
				.distinctUntilChanged()
		)
	}

	@Views(
		View(name = "by_hcparty_transport_guid_received", map = "classpath:js/message/By_hcparty_transport_guid_received_map.js"),
		View(name = "by_data_owner_transport_guid_received", map = "classpath:js/message/By_data_owner_transport_guid_received_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findMessagesByTransportGuidReceived(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		transportGuid: String?,
		paginationOffset: PaginationOffset<ComplexKey>
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = transportGuid?.takeIf { it.endsWith(":*") }?.let {
			val prefix = transportGuid.substring(0, transportGuid.length - 1)
			ComplexKey.of(partyId, prefix, null)
		} ?: ComplexKey.of(partyId, transportGuid, null)
		val endKey = transportGuid?.takeIf { it.endsWith(":*") }?.let {
			val prefix = transportGuid.substring(0, transportGuid.length - 1)
			ComplexKey.of(partyId, prefix + "\ufff0", ComplexKey.emptyObject())
		} ?: ComplexKey.of(partyId, transportGuid, ComplexKey.emptyObject())

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_transport_guid_received",
			"by_data_owner_transport_guid_received" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			paginationOffset,
			false
		)
		emitAll(client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}, { (it.components[2] as? Number)?.toLong() })))
	}

	@Views(
    	View(name = "by_hcparty_transport_guid_sent_date", map = "classpath:js/message/By_hcparty_transport_guid_sent_date.js"),
    	View(name = "by_data_owner_transport_guid_sent_date", map = "classpath:js/message/By_data_owner_transport_guid_sent_date.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findMessagesByTransportGuidAndSentDate(datastoreInformation: IDatastoreInformation, partyId: String, transportGuid: String, fromDate: Long, toDate: Long, paginationOffset: PaginationOffset<ComplexKey>) =
		flow {
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
				false
			)
			emitAll(client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}, { (it.components[2] as? Number)?.toLong() })))
		}

	@Views(
		View(name = "by_hcparty", map = "classpath:js/message/By_hcParty_map.js"),
		View(name = "by_data_owner", map = "classpath:js/message/By_dataOwner_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findMessagesByHcPartySortedByReceived(
		datastoreInformation: IDatastoreInformation,
		partyId: String,
		paginationOffset: PaginationOffset<ComplexKey>
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
			false
		).reduce(false)
		emitAll(
			client
				.interleave<ComplexKey, String?, Message>(
					viewQueries,
					compareBy({it.components[0] as? String}, { (it.components[1] as? Number)?.toLong() }),
				)
		)
	}

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
			"by_hcparty_patientfk",
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION
		).keys(keys).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({it.components[0] as String}, {it.components[1] as String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Message>>().map { it.doc }.distinctUntilChangedBy { it.id })
	}.distinctById()

	override fun listMessagesByHcPartyAndPatient(
		datastoreInformation: IDatastoreInformation,
		hcPartyId: String,
		secretPatientKey: String,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val key = ComplexKey.of(hcPartyId, secretPatientKey)
		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_patientfk",
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION,
			key, key, paginationOffset,false
		)
		emitAll(client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({it.components[0] as String}, {it.components[1] as String})))
	}

	@View(name = "by_parent_id", map = "classpath:js/message/By_parent_id_map.js")
	override fun getChildren(datastoreInformation: IDatastoreInformation, messageId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocs<String, Int, Message>(createQuery(datastoreInformation, "by_parent_id").includeDocs(true).key(messageId)).map { it.doc })
	}

	override fun getMessagesChildren(datastoreInformation: IDatastoreInformation, parentIds: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val byParentId = client.queryViewIncludeDocs<String, Int, Message>(createQuery(
			datastoreInformation,
			"by_parent_id"
		).includeDocs(true).keys(parentIds)).map { it.doc }.toList()
		emitAll(parentIds.asFlow().map { parentId -> byParentId.filter { message -> message.id == parentId } })
	}

	override fun findMessagesByIds(
		datastoreInformation: IDatastoreInformation,
		messageIds: Collection<String>
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(messageIds, Message::class.java))
	}

	@View(name = "by_invoice_id", map = "classpath:js/message/By_invoice_id_map.js")
	override fun listMessagesByInvoiceIds(datastoreInformation: IDatastoreInformation, invoiceIds: Set<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocs<String, Int, Message>(createQuery(datastoreInformation, "by_invoice_id").includeDocs(true).keys(invoiceIds)).map { it.doc })
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
			"by_data_owner_transport_guid" to DATA_OWNER_PARTITION
		).keys(HashSet(transportGuids).map { ComplexKey.of(hcPartyId, it) }).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Message>(viewQueries, compareBy({it.components[0] as String}, {it.components[1] as String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Message>>().map { it.doc }.distinctUntilChangedBy { it.id })

	}

	@View(name = "by_external_ref", map = "classpath:js/message/By_hcparty_external_ref_map.js")
	override fun getMessagesByExternalRefs(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, externalRefs: Set<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocs<Array<String>, String, Message>(
				createQuery(datastoreInformation, "by_hcparty_transport_guid")
					.includeDocs(true)
					.keys(externalRefs.flatMap {
						searchKeys.map { key -> ComplexKey.of(key, it) }
					})
			).map { it.doc }
		)
	}.distinctById()

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Message' && !doc.deleted && doc._conflicts) emit(doc._id )}")
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocsNoValue<String, Message>(createQuery(datastoreInformation, "conflicts").includeDocs(true)).map { it.doc })
	}

}
