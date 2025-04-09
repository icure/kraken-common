/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
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
import org.taktik.icure.asyncdao.InvoiceDAO
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.embed.InvoiceType
import org.taktik.icure.entities.embed.MediumType
import org.taktik.icure.utils.distinct
import org.taktik.icure.utils.distinctById
import org.taktik.icure.utils.interleave

@Repository("invoiceDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Invoice' && !doc.deleted) emit( null, doc._id )}")
class InvoiceDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericIcureDAOImpl<Invoice>(Invoice::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), InvoiceDAO {

	@Views(
	    View(name = "by_hcparty_date", map = "classpath:js/invoice/By_hcparty_date_map.js"),
	    View(name = "by_data_owner_date", map = "classpath:js/invoice/By_data_owner_date_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findInvoicesByHcParty(datastoreInformation: IDatastoreInformation, hcParty: String, fromDate: Long?, toDate: Long?, paginationOffset: PaginationOffset<ComplexKey>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(hcParty, fromDate)
		val endKey = ComplexKey.of(hcParty, toDate ?: ComplexKey.emptyObject())

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_date",
			"by_data_owner_date" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			paginationOffset,
			false
		)
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy({it.components[0] as? String}, {(it.components[1] as? Number)?.toLong()})))
	}

	@Views(
	    View(name = "by_hcparty_contact", map = "classpath:js/invoice/By_hcparty_contact_map.js"),
	    View(name = "by_data_owner_contact", map = "classpath:js/invoice/By_data_owner_contact_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartyAndContacts(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, contactId: Set<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_contact",
			"by_data_owner_contact" to DATA_OWNER_PARTITION
		)
			.keys(contactId.flatMap {
				searchKeys.map { key -> ComplexKey.of(key, it) }
			}).doNotIncludeDocs()
		val invoiceIds = client.interleave<ComplexKey, String>(viewQueries, compareBy({it.components[0] as String}, {it.components[1] as String}))
			.filterIsInstance<ViewRowNoDoc<Array<String>, String>>().map { it.id }

		emitAll(getEntities(datastoreInformation, invoiceIds.distinct()))
	}

	@Views(
		View(name = "by_hcparty_decision_reference", map = "classpath:js/invoice/By_hcparty_decision_reference_map.js", secondaryPartition = MAURICE_PARTITION),
		View(name = "by_data_owner_decision_reference_map", map = "classpath:js/invoice/By_data_owner_decision_reference_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoiceIdsByDataOwnerDecisionReference(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		decisionReference: String
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_decision_reference" to MAURICE_PARTITION,
			"by_data_owner_decision_reference_map" to DATA_OWNER_PARTITION
		).keys(searchKeys.map { key -> ComplexKey.of(key, decisionReference) }).doNotIncludeDocs()
		emitAll(
			client.interleave<ComplexKey, String>(viewQueries, compareBy({it.components[0] as String}, {it.components[1] as String}))
				.filterIsInstance<ViewRowNoDoc<Array<String>, String>>()
				.map { it.id }
		)
	}

	@Views(
	    View(name = "by_hcparty_reference", map = "classpath:js/invoice/By_hcparty_reference_map.js"),
	    View(name = "by_data_owner_reference", map = "classpath:js/invoice/By_data_owner_reference_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartyAndReferences(datastoreInformation: IDatastoreInformation, hcParty: String, invoiceReferences: Set<String>?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_reference",
			"by_data_owner_reference" to DATA_OWNER_PARTITION
		).let { viewQuery ->
				if (invoiceReferences != null)
					viewQuery.keys(invoiceReferences.map { ComplexKey.of(hcParty, it) })
				else
					viewQuery.startKey(ComplexKey.of(hcParty, null)).endKey(ComplexKey.of(hcParty, ComplexKey.emptyObject()))
			}.includeDocs()
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy({it.components[0] as String?}, {it.components[1] as? String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Invoice>>().map { it.doc }.distinctById())
	}

	@Views(
	    View(name = "by_hcparty_reference", map = "classpath:js/invoice/By_hcparty_reference_map.js"),
	    View(name = "by_data_owner_reference", map = "classpath:js/invoice/By_data_owner_reference_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartyAndReferences(datastoreInformation: IDatastoreInformation, hcParty: String, from: String?, to: String?, descending: Boolean, limit: Int) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(hcParty, if (descending && from == null) ComplexKey.emptyObject() else from)
		val endKey = ComplexKey.of(hcParty, if (!descending && to == null) ComplexKey.emptyObject() else to)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_reference",
			"by_data_owner_reference" to DATA_OWNER_PARTITION
		)
			.startKey(startKey).startKey(endKey).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Invoice>>().map { it.doc })
	}

	@Views(
	    View(name = "by_hcparty_groupid", map = "classpath:js/invoice/By_hcparty_groupid_map.js"),
	    View(name = "by_data_owner_groupid", map = "classpath:js/invoice/By_data_owner_groupid_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartyAndGroupId(
		datastoreInformation: IDatastoreInformation,
		hcParty: String,
		inputGroupId: String
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_groupid",
			"by_data_owner_groupid" to DATA_OWNER_PARTITION
		)
			.key(ComplexKey.of(hcParty, inputGroupId)).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Invoice>>().map { it.doc })
	}

	@Views(
	    View(name = "by_hcparty_recipient", map = "classpath:js/invoice/By_hcparty_recipient_map.js"),
	    View(name = "by_data_owner_recipient", map = "classpath:js/invoice/By_data_owner_recipient_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartyAndRecipientIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, recipientIds: Set<String?>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_recipient",
			"by_data_owner_recipient" to DATA_OWNER_PARTITION
		)
			.keys(recipientIds.flatMap { id ->
				searchKeys.map { ComplexKey.of(it, id) } }
			).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Invoice>>().map { it.doc })
	}.distinctById()

	@Deprecated("This method cannot include results with secure delegations, use listInvoiceIdsByDataOwnerPatientInvoiceDate instead")
	@Views(
	    View(name = "by_hcparty_patientfk", map = "classpath:js/invoice/By_hcparty_patientfk_map.js"),
	    View(name = "by_data_owner_patientfk", map = "classpath:js/invoice/By_data_owner_patientfk_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartyAndPatientSfks(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: Set<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patientfk",
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION
		)
			.keys(secretPatientKeys.flatMap { fk ->
				searchKeys.map { ComplexKey.of(it, fk) }
			}).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Invoice>>().map { it.doc })
	}.distinctById()

	@View(name = "by_hcparty_patientfk_date", map = "classpath:js/invoice/By_hcparty_patientfk_date_map.js", secondaryPartition = MAURICE_PARTITION)
	override fun listInvoiceIdsByDataOwnerPatientInvoiceDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = getEntityIdsByDataOwnerPatientDate(
		views = listOf("by_hcparty_patientfk_date" to MAURICE_PARTITION, "by_data_owner_patientfk" to DATA_OWNER_PARTITION),
		datastoreInformation = datastoreInformation,
		searchKeys = searchKeys,
		secretForeignKeys = secretForeignKeys,
		startDate = startDate,
		endDate = endDate,
		descending = descending
	)

	@Views(
	    View(name = "by_hcparty_recipient_unsent", map = "classpath:js/invoice/By_hcparty_recipient_unsent_map.js"),
	    View(name = "by_data_owner_recipient_unsent", map = "classpath:js/invoice/By_data_owner_recipient_unsent_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartyAndRecipientIdsUnsent(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, recipientIds: Set<String?>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_recipient_unsent",
			"by_data_owner_recipient_unsent" to DATA_OWNER_PARTITION
		)
			.keys(recipientIds.flatMap { id ->
				searchKeys.map { ComplexKey.of(it, id) }
			}).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Invoice>>().map { it.doc })
	}.distinctById()

	@Views(
	    View(name = "by_hcparty_patientfk_unsent", map = "classpath:js/invoice/By_hcparty_patientfk_unsent_map.js"),
	    View(name = "by_data_owner_patientfk_unsent", map = "classpath:js/invoice/By_data_owner_patientfk_unsent_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartyAndPatientFkUnsent(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: Set<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patientfk_unsent",
			"by_data_owner_patientfk_unsent" to DATA_OWNER_PARTITION
		)
			.keys(secretPatientKeys.flatMap { fk ->
				searchKeys.map { ComplexKey.of(it, fk) }
			}).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Invoice>>().map { it.doc })

	}.distinctById()

	@Views(
	    View(name = "by_hcparty_sentmediumtype_invoicetype_sent_date", map = "classpath:js/invoice/By_hcparty_sentmediumtype_invoicetype_sent_date.js"),
	    View(name = "by_data_owner_sentmediumtype_invoicetype_sent_date", map = "classpath:js/invoice/By_data_owner_sentmediumtype_invoicetype_sent_date.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(datastoreInformation: IDatastoreInformation, hcParty: String, sentMediumType: MediumType, invoiceType: InvoiceType, sent: Boolean, fromDate: Long?, toDate: Long?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(hcParty, sentMediumType, invoiceType, sent, fromDate)
		val endKey = ComplexKey.of(hcParty, sentMediumType, invoiceType, sent, toDate ?: ComplexKey.emptyObject())

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_sentmediumtype_invoicetype_sent_date",
			"by_data_owner_sentmediumtype_invoicetype_sent_date" to DATA_OWNER_PARTITION
		)
			.startKey(startKey).endKey(endKey).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy(
			{it.components[0] as? String},
			{(it.components[1] as? MediumType)?.name ?: it.components[1] as? String},
			{(it.components[2] as? InvoiceType)?.name ?: it.components[2] as? String},
			{it.components[3] as? Boolean ?: it.components[3] as? String},
			{it.components[4] as? Long ?: it.components[4]?.let { c -> if (c == ComplexKey.emptyObject()) Long.MAX_VALUE else null }},
		)).filterIsInstance<ViewRowWithDoc<ComplexKey, String, Invoice>>().map { it.doc })
	}

	@Views(
	    View(name = "by_hcparty_sending_mode_status_date", map = "classpath:js/invoice/By_hcparty_sending_mode_status_date.js"),
	    View(name = "by_data_owner_sending_mode_status_date", map = "classpath:js/invoice/By_data_owner_sending_mode_status_date.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listInvoicesByHcPartySendingModeStatus(datastoreInformation: IDatastoreInformation, hcParty: String, sendingMode: String?, status: String?, fromDate: Long?, toDate: Long?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		var startKey = ComplexKey.of(hcParty)
		var endKey = ComplexKey.of(hcParty, ComplexKey.emptyObject(), ComplexKey.emptyObject(), ComplexKey.emptyObject())
		if (fromDate != null && toDate != null) { // The full key is given
			startKey = ComplexKey.of(hcParty, sendingMode, status, fromDate)
			endKey = ComplexKey.of(hcParty, sendingMode, status, toDate)
		} else if (status != null) {
			startKey = ComplexKey.of(hcParty, sendingMode, status)
			endKey = ComplexKey.of(hcParty, sendingMode, status, ComplexKey.emptyObject())
		} else if (sendingMode != null) {
			startKey = ComplexKey.of(hcParty, sendingMode)
			endKey = ComplexKey.of(hcParty, sendingMode, ComplexKey.emptyObject(), ComplexKey.emptyObject())
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_sending_mode_status_date",
			"by_data_owner_sending_mode_status_date" to DATA_OWNER_PARTITION
		)
			.startKey(startKey).endKey(endKey).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Invoice>(viewQueries, compareBy(
			{it.components[0] as? String},
			{it.components[1] as? String ?: it.components[1]?.let { c -> if (c == ComplexKey.emptyObject()) "\uFF0F" else null }},
			{it.components[2] as? String ?: it.components[2]?.let { c -> if (c == ComplexKey.emptyObject()) "\uFF0F" else null}},
			{it.components[3] as? Long ?: it.components[3]?.let { c -> if (c == ComplexKey.emptyObject()) "\uFF0F" else null }},
		)).filterIsInstance<ViewRowWithDoc<ComplexKey, String, Invoice>>().map { it.doc })
	}

	@View(name = "by_serviceid", map = "classpath:js/invoice/By_serviceid_map.js")
	override fun listInvoicesByServiceIds(datastoreInformation: IDatastoreInformation, serviceIds: Set<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocs<String, String, Invoice>(createQuery(datastoreInformation, "by_serviceid").includeDocs(true).keys(serviceIds)).map { it.doc })
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	@View(name = "by_status_hcps_sentdate", map = "classpath:js/invoice/By_status_hcps_sentdate_map.js")
	override fun listInvoicesHcpsByStatus(datastoreInformation: IDatastoreInformation, status: String, from: Long?, to: Long?, hcpIds: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val result = hcpIds.map { hcpId ->
			client.queryView<Array<String>, String>(
				createQuery(datastoreInformation, "by_status_hcps_sentdate").includeDocs(false)
					.startKey(ComplexKey.of(status, hcpId, from))
					.endKey(ComplexKey.of(status, hcpId, to ?: ComplexKey.emptyObject()))
			).mapNotNull { it.value }
		}.asFlow().flattenConcat().distinct()

		emitAll(getEntities(datastoreInformation, result))
	}

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Invoice' && !doc.deleted && doc._conflicts) emit(doc._id )}")
	override fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Invoice> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocsNoValue<String, Invoice>(createQuery(datastoreInformation, "conflicts").includeDocs(true)).mapNotNull { it.doc })
	}

	@View(name = "tarification_by_data_owner", map = "classpath:js/invoice/Tarification_by_data_owner_code.js", reduce = "_count", secondaryPartition = DATA_OWNER_PARTITION)
	override fun listInvoiceIdsByTarificationsAndCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeCode: String?, startValueDate: Long?, endValueDate: Long?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startDate = if (startValueDate != null && startValueDate < 99999999) {
			startValueDate * 1000000
		} else startValueDate
		val endDate = if (endValueDate != null && endValueDate < 99999999) {
			endValueDate * 1000000
		} else endValueDate
		val from = ComplexKey.of(
			hcPartyId,
			codeCode,
			startDate
		)
		val to = ComplexKey.of(
			hcPartyId,
			codeCode ?: ComplexKey.emptyObject(),
			endDate ?: ComplexKey.emptyObject()
		)

		emitAll(client.queryView<Array<String>, String>(createQuery(
			datastoreInformation,
			"tarification_by_data_owner"
		).includeDocs(false).startKey(from).endKey(to).reduce(false)).mapNotNull { it.value })
	}

	override fun listInvoiceIdsByTarificationsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeCode: String?, startValueDate: Long?, endValueDate: Long?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startDate = if (startValueDate != null && startValueDate < 99999999) {
			startValueDate * 1000000
		} else startValueDate
		val endDate = if (endValueDate != null && endValueDate < 99999999) {
			endValueDate * 1000000
		} else endValueDate

		val from = ComplexKey.of(
			hcPartyId,
			codeCode,
			startDate
		)
		val to = ComplexKey.of(
			hcPartyId,
			codeCode ?: ComplexKey.emptyObject(),
			endDate ?: ComplexKey.emptyObject()
		)

		emitAll(client.queryView<Array<String>, String>(createQuery(
			datastoreInformation,
			"tarification_by_data_owner"
		).includeDocs(false).startKey(from).endKey(to).reduce(false)).mapNotNull { it.id })
	}

	override fun listTarificationsFrequencies(datastoreInformation: IDatastoreInformation, hcPartyId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(
			hcPartyId, null
		)
		val to = ComplexKey.of(
			hcPartyId,
			ComplexKey.emptyObject()
		)

		emitAll(client.queryView<ComplexKey, Long>(createQuery(datastoreInformation, "tarification_by_data_owner").startKey(from).endKey(to).includeDocs(false).reduce(true).group(true).groupLevel(2)))
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_patientfk" to DATA_OWNER_PARTITION)
			Partitions.Maurice -> warmup(datastoreInformation, "by_hcparty_patientfk_date" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
