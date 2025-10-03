/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ArrayNode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.Client
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
import org.taktik.icure.asyncdao.BEPPE_PARTITION
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.ContactIdServiceId
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.utils.DeduplicationMode
import org.taktik.icure.utils.FuzzyDates
import org.taktik.icure.utils.NoDocViewQueries
import org.taktik.icure.utils.distinct
import org.taktik.icure.utils.distinctById
import org.taktik.icure.utils.distinctByIdIf
import org.taktik.icure.utils.distinctIf
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.main
import kotlin.String
import kotlin.collections.map
import kotlin.collections.set

@Repository("contactDAO")
@Profile("app")
@Views(
	View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Contact' && !doc.deleted) emit( null, doc._id )}"),
	View(name = "by_service", map = "classpath:js/contact/By_service.js"), // Legacy
)
class ContactDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
) : GenericDAOImpl<Contact>(Contact::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig),
	ContactDAO {

	override suspend fun getContact(datastoreInformation: IDatastoreInformation, id: String): Contact? = get(datastoreInformation, id)

	override fun getContacts(datastoreInformation: IDatastoreInformation, contactIds: Flow<String>): Flow<Contact> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.get(contactIds, Contact::class.java))
	}

	override fun getContacts(datastoreInformation: IDatastoreInformation, contactIds: Collection<String>): Flow<Contact> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.get(contactIds, Contact::class.java))
	}

	@Views(
		View(name = "by_hcparty_openingdate", map = "classpath:js/contact/By_hcparty_openingdate.js"),
		View(name = "by_data_owner_openingdate", map = "classpath:js/contact/By_data_owner_openingdate.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactsByOpeningDate(datastoreInformation: IDatastoreInformation, hcPartyId: String, startOpeningDate: Long?, endOpeningDate: Long?, pagination: PaginationOffset<ComplexKey>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(hcPartyId, startOpeningDate)
		val endKey = ComplexKey.of(hcPartyId, endOpeningDate ?: ComplexKey.emptyObject())

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_openingdate",
			"by_data_owner_openingdate" to DATA_OWNER_PARTITION,
			startKey,
			endKey,
			pagination,
			false,
		)
		emitAll(client.interleave<ComplexKey, String, Contact>(viewQueries, compareBy({ it.components[0] as String }, { (it.components[1] as? Number)?.toLong() })))
	}

	override fun listContactIdsByOpeningDate(datastoreInformation: IDatastoreInformation, hcPartyId: String, startOpeningDate: Long?, endOpeningDate: Long?, descending: Boolean): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey =
			if (descending) {
				ComplexKey.of(hcPartyId, endOpeningDate ?: ComplexKey.emptyObject())
			} else {
				ComplexKey.of(hcPartyId, startOpeningDate)
			}
		val endKey =
			if (descending) {
				ComplexKey.of(hcPartyId, startOpeningDate)
			} else {
				ComplexKey.of(hcPartyId, endOpeningDate ?: ComplexKey.emptyObject())
			}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_openingdate".main(),
			"by_data_owner_openingdate" to DATA_OWNER_PARTITION,
		)
			.startKey(startKey)
			.endKey(endKey)
			.descending(descending)
			.doNotIncludeDocs()
		emitAll(
			client
				.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as String }, { (it.components[1] as? Number)?.toLong() }))
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.map { it.id },
		)
	}

	@Views(
		View(name = "by_hcparty", map = "classpath:js/contact/By_hcparty.js"),
		View(name = "by_data_owner", map = "classpath:js/contact/By_data_owner.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findContactsByHcParty(datastoreInformation: IDatastoreInformation, hcPartyId: String, pagination: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty",
			"by_data_owner" to DATA_OWNER_PARTITION,
			hcPartyId,
			hcPartyId,
			pagination,
			false,
		)
		emitAll(client.interleave<String, String, Contact>(viewQueries, compareBy { it }))
	}

	@Views(
		View(name = "by_hcparty_identifier", map = "classpath:js/contact/By_hcparty_identifier.js"),
		View(name = "by_data_owner_identifier", map = "classpath:js/contact/By_data_owner_identifier.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = identifiers.flatMap { identifier ->
			searchKeys.map { key -> arrayOf(key, identifier.system, identifier.value) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_identifier",
			"by_data_owner_identifier" to DATA_OWNER_PARTITION,
		).keys(keys).doNotIncludeDocs()
		emitAll(client.interleave<Array<String>, String>(viewQueries, compareBy({ it[0] }, { it[1] }, { it[2] })).filterIsInstance<ViewRowNoDoc<Array<String>, String>>().map { it.id })
	}.distinct()

	override fun findContactsByIds(datastoreInformation: IDatastoreInformation, contactIds: Flow<String>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(contactIds, Contact::class.java))
	}

	override fun findContactsByIds(datastoreInformation: IDatastoreInformation, contactIds: Collection<String>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(contactIds, Contact::class.java))
	}

	override fun listContactIdsByHealthcareParty(datastoreInformation: IDatastoreInformation, hcPartyId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(datastoreInformation, "by_hcparty", "by_data_owner" to DATA_OWNER_PARTITION).startKey(hcPartyId)
			.endKey(hcPartyId)
			.doNotIncludeDocs()
		emitAll(client.interleave<String, String>(viewQueries, compareBy { it }).filterIsInstance<ViewRowNoDoc<Array<String>, String>>().map { it.id }.distinctUntilChanged())
	}

	@Deprecated("This method is inefficient for high volumes of keys, use listContactIdsByDataOwnerPatientOpeningDate instead")
	@Views(
		View(name = "by_hcparty_patientfk", map = "classpath:js/contact/By_hcparty_patientfk_map.js"),
		View(name = "by_data_owner_patientfk", map = "classpath:js/contact/By_data_owner_patientfk_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { key -> arrayOf(key, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patientfk".main(),
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION,
		).keys(keys).includeDocs()
		emitAll(
			relink(
				client.interleave<Array<String>, String, Contact>(viewQueries, compareBy({ it[0] }, { it[1] }))
					.filterIsInstance<ViewRowWithDoc<Array<String>, String, Contact>>().map { it.doc },
			),
		)
	}.distinctById()

	@View(name = "by_hcparty_patientfk_openingdate", map = "classpath:js/contact/By_hcparty_patientfk_openingdate_map.js", secondaryPartition = MAURICE_PARTITION)
	override fun listContactIdsByDataOwnerPatientOpeningDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = getEntityIdsByDataOwnerPatientDate(
		views = listOf("by_hcparty_patientfk_openingdate" to MAURICE_PARTITION, "by_data_owner_patientfk" to DATA_OWNER_PARTITION),
		datastoreInformation = datastoreInformation,
		searchKeys = searchKeys,
		secretForeignKeys = secretForeignKeys,
		startDate = startDate,
		endDate = endDate,
		descending = descending,
	)

	@Views(
		View(name = "by_hcparty_patientfk", map = "classpath:js/contact/By_hcparty_patientfk_map.js"),
		View(name = "by_data_owner_patientfk", map = "classpath:js/contact/By_data_owner_patientfk_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactIdsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { key -> arrayOf(key, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patientfk",
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION,
		).keys(keys).doNotIncludeDocs()
		emitAll(
			client.interleave<Array<String>, String>(viewQueries, compareBy({ it[0] }, { it[1] }))
				.filterIsInstance<ViewRowNoDoc<Array<String>, String>>().mapNotNull { it.id },
		)
	}.distinct()

	private suspend fun createQueriesForSearchKeysAndFormIds(
		datastoreInformation: IDatastoreInformation,
		formIds: List<String>,
		searchKeys: Set<String>,
	) = createQueries(
		datastoreInformation,
		"by_hcparty_formid",
		"by_data_owner_formid" to DATA_OWNER_PARTITION,
	).keys(
		formIds.flatMap { k ->
			searchKeys.map { arrayOf(it, k) }
		},
	)

	@Views(
		View(name = "by_hcparty_formid", map = "classpath:js/contact/By_hcparty_formid_map.js"),
		View(name = "by_data_owner_formid", map = "classpath:js/contact/By_data_owner_formid_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactsByHcPartyAndFormId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, formId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueriesForSearchKeysAndFormIds(datastoreInformation, listOf(formId), searchKeys).includeDocs()
		emitAll(
			relink(
				client.interleave<Array<String>, String, Contact>(viewQueries, compareBy({ it[0] }, { it[1] }))
					.filterIsInstance<ViewRowWithDoc<Array<String>, String, Contact>>().map { it.doc },
			),
		)
	}.distinctByIdIf(searchKeys.size > 1)

	override fun listContactsByHcPartyAndFormIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, ids: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueriesForSearchKeysAndFormIds(datastoreInformation, ids, searchKeys).doNotIncludeDocs()
		val result = client.interleave<Array<String>, String>(viewQueries, compareBy({ it[0] }, { it[1] }))
			.filterIsInstance<ViewRowNoDoc<Array<String>, String>>().mapNotNull { it.id }.distinct()

		emitAll(relink(getContacts(datastoreInformation, result)))
	}

	override fun listContactIdsByDataOwnerAndFormIds(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		formIds: List<String>,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueriesForSearchKeysAndFormIds(datastoreInformation, formIds, searchKeys).doNotIncludeDocs()
		client.interleave<Array<String>, String>(viewQueries, compareBy({ it[0] }, { it[1] }))
			.filterIsInstance<ViewRowNoDoc<Array<String>, String>>()
			.mapNotNull { it.id }
			.distinct().also {
				emitAll(it)
			}
	}

	@Views(
		View(name = "by_hcparty_serviceid", map = "classpath:js/contact/By_hcparty_serviceid_map.js"),
		View(name = "by_data_owner_serviceid", map = "classpath:js/contact/By_data_owner_serviceid_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactsByHcPartyAndServiceId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, serviceId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_serviceid",
			"by_data_owner_serviceid" to DATA_OWNER_PARTITION,
		).keys(searchKeys.map { arrayOf(it, serviceId) }).includeDocs()
		emitAll(
			relink(
				client.interleave<Array<String>, String, Contact>(viewQueries, compareBy({ it[0] }, { it[1] }))
					.filterIsInstance<ViewRowWithDoc<Array<String>, String, Contact>>().map { it.doc },
			),
		)
	}.distinctByIdIf(searchKeys.size > 1)

	override fun findContactsByHcPartyServiceId(datastoreInformation: IDatastoreInformation, hcPartyId: String, serviceId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_serviceid",
			"by_data_owner_serviceid" to DATA_OWNER_PARTITION,
		).key(ComplexKey.of(hcPartyId, serviceId)).includeDocs()
		emitAll(
			relink(
				client.interleave<ComplexKey, String, Contact>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }))
					.filterIsInstance<ViewRowWithDoc<Array<String>, String, Contact>>().map { it.doc },
			),
		)
	}

	@View(name = "service_by_linked_id", map = "classpath:js/contact/Service_by_linked_id.js", secondaryPartition = MAURICE_PARTITION)
	override fun findServiceIdsByIdQualifiedLink(datastoreInformation: IDatastoreInformation, linkValues: List<String>, linkQualification: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "service_by_linked_id", MAURICE_PARTITION)
			.keys(linkValues)
			.includeDocs(false)
		val res = client.queryView<String, Array<String>>(viewQuery)
		emitAll(
			(linkQualification?.let { lt -> res.filter { it.value!![0] == lt } } ?: res)
				.map { it.value!![1] },
		)
	}

	@View(name = "service_by_association_id", map = "classpath:js/contact/Service_by_association_id.js")
	override fun listServiceIdsByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "service_by_association_id")
			.key(associationId)
			.includeDocs(false)
		emitAll(
			client.queryView<String, String>(viewQuery).map {
				checkNotNull(it.value) { "A Service cannot have a null id" }
			},
		)
	}

	@Views(
		View(name = "service_by_hcparty", map = "classpath:js/contact/Service_by_hcparty_map.js"),
		View(name = "service_by_data_owner", map = "classpath:js/contact/Service_by_data_owner_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listServiceIdsByHcParty(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createQueries(
			datastoreInformation,
			"service_by_hcparty",
			"service_by_data_owner" to DATA_OWNER_PARTITION,
		)
			.keys(searchKeys)
			.doNotIncludeDocs()
		emitAll(client.interleave<String, String>(viewQueries, compareBy { it }, DeduplicationMode.ID_AND_VALUE).filterIsInstance<ViewRowNoDoc<String, String>>().mapNotNull { it.value })
	}.distinctIf(searchKeys.size > 1)

	@OptIn(ExperimentalCoroutinesApi::class)
	@View(name = "service_by_association_id", map = "classpath:js/contact/Service_by_association_id.js")
	override fun listServicesByAssociationId(datastoreInformation: IDatastoreInformation, associationId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "service_by_association_id")
			.key(associationId)
			.includeDocs(true)

		val res = client.queryViewIncludeDocs<String, String, Contact>(viewQuery)
		emitAll(
			res.mapNotNull { it.doc }
				.flatMapConcat { contact ->
					contact.services.filter { service ->
						service.qualifiedLinks.values.flatMap { it.keys }.contains(associationId)
					}.asFlow()
				},
		)
	}

	private suspend fun createByHcPartyTagDateQueries(
		datastoreInformation: IDatastoreInformation,
		mainView: String,
		secondaryView: String,
		hcPartyId: String,
		type: String?,
		code: String?,
		startDate: Long? = null,
		endDate: Long? = null,
		descending: Boolean = false,
	): NoDocViewQueries {
		val from = ComplexKey.of(
			hcPartyId,
			type,
			code,
			startDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: startDate,
		)
		val to = ComplexKey.of(
			hcPartyId,
			type ?: ComplexKey.emptyObject(),
			code ?: ComplexKey.emptyObject(),
			endDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: endDate ?: ComplexKey.emptyObject(),
		)

		return createQueries(
			datastoreInformation,
			mainView,
			secondaryView to DATA_OWNER_PARTITION,
		)
			.startKey(if (descending) to else from)
			.endKey(if (descending) from else to)
			.descending(descending)
			.reduce(false)
			.doNotIncludeDocs()
	}

	@Views(
		View(name = "service_by_hcparty_tag", map = "classpath:js/contact/Service_by_hcparty_tag.js"),
		View(name = "service_by_data_owner_tag", map = "classpath:js/contact/Service_by_data_owner_tag.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listServiceIdsByTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createByHcPartyTagDateQueries(
			datastoreInformation = datastoreInformation,
			mainView = "service_by_hcparty_tag",
			secondaryView = "service_by_data_owner_tag",
			hcPartyId = hcPartyId,
			type = tagType,
			code = tagCode,
			startDate = startValueDate,
			endDate = endValueDate,
			descending = descending,
		)

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ it.components[2] as? String },
					{ (it.components[3] as? Number)?.toLong() },
				),
				DeduplicationMode.ID_AND_VALUE,
			).filterIsInstance<ViewRowNoDoc<String, String>>().mapNotNull { it.value }.distinct(),
		)
	}

	override fun listContactIdsByServiceTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createByHcPartyTagDateQueries(
			datastoreInformation = datastoreInformation,
			mainView = "service_by_hcparty_tag",
			secondaryView = "service_by_data_owner_tag",
			hcPartyId = hcPartyId,
			type = tagType,
			code = tagCode,
		)

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ it.components[2] as? String },
					{ (it.components[3] as? Number)?.toLong() },
				),
				DeduplicationMode.ID,
			).filterIsInstance<ViewRowNoDoc<String, String>>().mapNotNull { it.id }.distinct(),
		)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	@Views(
		View(name = "service_by_hcparty_patient_tag", map = "classpath:js/contact/Service_by_hcparty_patient_tag.js"),
		View(name = "service_by_data_owner_patient_tag", map = "classpath:js/contact/Service_by_data_owner_patient_tag.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listServiceIdsByPatientAndTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val canonicalStartValueDate = startValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: startValueDate
		val canonicalEndValueDate = endValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: endValueDate

		val idFlows = mutableListOf<Flow<String>>()
		for (patientSecretForeignKey in patientSecretForeignKeys) {
			val from = ComplexKey.of(
				hcPartyId,
				patientSecretForeignKey,
				tagType,
				tagCode,
				canonicalStartValueDate,
			)
			val to = ComplexKey.of(
				hcPartyId,
				patientSecretForeignKey,
				tagType ?: ComplexKey.emptyObject(),
				tagCode ?: ComplexKey.emptyObject(),
				canonicalEndValueDate ?: ComplexKey.emptyObject(),
			)

			val viewQueries = createQueries(
				datastoreInformation,
				"service_by_hcparty_patient_tag",
				"service_by_data_owner_patient_tag" to DATA_OWNER_PARTITION,
			)
				.startKey(if (descending) to else from)
				.endKey(if (descending) from else to)
				.descending(descending)
				.doNotIncludeDocs()

			idFlows.add(
				client.interleave<ComplexKey, String>(
					viewQueries,
					compareBy(
						{ it.components[0] as? String },
						{ it.components[1] as? String },
						{ it.components[2] as? String },
						{ it.components[3] as? String },
						{ (it.components[4] as? Number)?.toLong() },
					),
					DeduplicationMode.ID_AND_VALUE,
				).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().mapNotNull { it.value },
			)
		}
		emitAll(idFlows.asFlow().flattenConcat().distinct())
	}

	@Views(
		View(name = "service_by_hcparty_code", map = "classpath:js/contact/Service_by_hcparty_code.js", reduce = "_count"),
		View(name = "service_by_data_owner_code", map = "classpath:js/contact/Service_by_data_owner_code.js", reduce = "_count", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listServiceIdsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createByHcPartyTagDateQueries(
			datastoreInformation = datastoreInformation,
			mainView = "service_by_hcparty_code",
			secondaryView = "service_by_data_owner_code",
			hcPartyId = hcPartyId,
			type = codeType,
			code = codeCode,
			startDate = startValueDate,
			endDate = endValueDate,
			descending = descending,
		)

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ it.components[2] as? String },
					{ (it.components[3] as? Number)?.toLong() },
				),
				DeduplicationMode.ID_AND_VALUE,
			).filterIsInstance<ViewRowNoDoc<String, String>>().mapNotNull { it.value }.distinct(),
		)
	}

	override fun listContactIdsByServiceCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String, codeCode: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createByHcPartyTagDateQueries(
			datastoreInformation = datastoreInformation,
			mainView = "service_by_hcparty_code",
			secondaryView = "service_by_data_owner_code",
			hcPartyId = hcPartyId,
			type = codeType,
			code = codeCode,
		)

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ it.components[2] as? String },
					{ (it.components[3] as? Number)?.toLong() },
				),
				DeduplicationMode.ID,
			).filterIsInstance<ViewRowNoDoc<String, String>>().mapNotNull { it.id }.distinct(),
		)
	}

	@Views(
		View(name = "by_hcparty_tag", map = "classpath:js/contact/By_hcparty_tag.js", reduce = "_count"),
		View(name = "by_data_owner_tag", map = "classpath:js/contact/By_data_owner_tag.js", reduce = "_count", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactIdsByTag(datastoreInformation: IDatastoreInformation, hcPartyId: String, tagType: String?, tagCode: String?, startValueDate: Long?, endValueDate: Long?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val normalizedStartValueDate = if (startValueDate != null && startValueDate < 99999999) {
			startValueDate * 1000000
		} else {
			startValueDate
		}
		val normalizedEndValueDate = if (endValueDate != null && endValueDate < 99999999) {
			endValueDate * 1000000
		} else {
			endValueDate
		}
		val from = ComplexKey.of(
			hcPartyId,
			tagType,
			tagCode,
			normalizedStartValueDate,
		)
		val to = ComplexKey.of(
			hcPartyId,
			tagType ?: ComplexKey.emptyObject(),
			tagCode ?: ComplexKey.emptyObject(),
			normalizedEndValueDate ?: ComplexKey.emptyObject(),
		)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_tag",
			"by_data_owner_tag" to DATA_OWNER_PARTITION,
		)
			.startKey(from)
			.endKey(to)
			.reduce(false)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(
				viewQueries,
				compareBy(
					{ it.components[0] as? String },
					{ it.components[1] as? String },
					{ it.components[2] as? String },
					{ (it.components[3] as? Number)?.toLong() },
				),
				DeduplicationMode.ID_AND_VALUE,
			).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().mapNotNull { it.id }.distinct(),
		)
	}

	@Views(
		View(name = "service_id_by_hcparty_helements", map = "classpath:js/contact/Service_id_by_hcparty_helement_ids.js"),
		View(name = "service_id_by_data_owner_helements", map = "classpath:js/contact/Service_id_by_data_owner_helement_ids.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listServiceIdsByHcPartyHealthElementIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, healthElementIds: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createQueries(
			datastoreInformation,
			"service_id_by_hcparty_helements",
			"service_id_by_data_owner_helements" to DATA_OWNER_PARTITION,
		)
			.keys(
				healthElementIds.flatMap {
					searchKeys.map { key ->
						ComplexKey.of(key, it)
					}
				},
			)
			.doNotIncludeDocs()

		emitAll(
			client
				.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }), DeduplicationMode.ID_AND_VALUE)
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().mapNotNull { it.value },
		)
	}.distinct()

	@Views(
		View(name = "service_by_hcparty_identifier", map = "classpath:js/contact/Service_by_hcparty_identifier.js"),
		View(name = "service_by_data_owner_identifier", map = "classpath:js/contact/Service_by_data_owner_identifier.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listServiceIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"service_by_hcparty_identifier",
			"service_by_data_owner_identifier" to DATA_OWNER_PARTITION,
		)
			.keys(
				identifiers.flatMap {
					searchKeys.map { key ->
						ComplexKey.of(key, it.system, it.value)
					}
				},
			).doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }), DeduplicationMode.ID_AND_VALUE)
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.mapNotNull { it.value },
		)
	}.distinct()

	@Views(
		View(name = "by_hcparty_code", map = "classpath:js/contact/By_hcparty_code.js", reduce = "_count"),
		View(name = "by_data_owner_code", map = "classpath:js/contact/By_data_owner_code.js", reduce = "_count", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listContactIdsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val normalizedStartValueDate = if (startValueDate != null && startValueDate < 99999999) {
			startValueDate * 1000000
		} else {
			startValueDate
		}
		val normalizedEndValueDate = if (endValueDate != null && endValueDate < 99999999) {
			endValueDate * 1000000
		} else {
			endValueDate
		}
		val from = ComplexKey.of(
			hcPartyId,
			codeType,
			codeCode,
			normalizedStartValueDate,
		)
		val to = ComplexKey.of(
			hcPartyId,
			codeType ?: ComplexKey.emptyObject(),
			codeCode ?: ComplexKey.emptyObject(),
			normalizedEndValueDate ?: ComplexKey.emptyObject(),
		)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_code",
			"by_data_owner_code" to DATA_OWNER_PARTITION,
		)
			.startKey(from)
			.endKey(to)
			.reduce(false)
			.doNotIncludeDocs()

		emitAll(
			client.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as? String }, { it.components[1] as? String }), DeduplicationMode.ID_AND_VALUE)
				.filterIsInstance<ViewRowNoDoc<Array<String>, String>>().map { it.id },
		)
	}

	override fun listCodesFrequencies(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeType: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(
			hcPartyId,
			codeType,
			null,
		)
		val to = ComplexKey.of(
			hcPartyId,
			codeType,
			ComplexKey.emptyObject(),
		)

		val viewQuery = createQuery(datastoreInformation, "service_by_hcparty_code").startKey(from).endKey(to).includeDocs(false).reduce(true).group(true).groupLevel(3)

		emitAll(client.queryView<Array<String>, Long>(viewQuery).map { Pair(ComplexKey.of(*(it.key as Array<String>)), it.value) })
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	@Views(
		View(name = "service_by_hcparty_patient_code", map = "classpath:js/contact/Service_by_hcparty_patient_code.js"),
		View(name = "service_by_data_owner_patient_code", map = "classpath:js/contact/Service_by_data_owner_patient_code.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listServicesIdsByPatientAndCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, patientSecretForeignKeys: List<String>, codeType: String?, codeCode: String?, startValueDate: Long?, endValueDate: Long?, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val canonicalStartValueDate = startValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: startValueDate
		val canonicalEndValueDate = endValueDate?.takeIf { it < 99999999 }?.let { it * 1000000 } ?: endValueDate

		val idFlows = mutableListOf<Flow<String>>()
		for (patientSecretForeignKey in patientSecretForeignKeys) {
			val from = ComplexKey.of(
				hcPartyId,
				patientSecretForeignKey,
				codeType,
				codeCode,
				canonicalStartValueDate,
			)
			val to = ComplexKey.of(
				hcPartyId,
				patientSecretForeignKey,
				codeType ?: ComplexKey.emptyObject(),
				codeCode ?: ComplexKey.emptyObject(),
				canonicalEndValueDate ?: ComplexKey.emptyObject(),
			)

			val viewQueries = createQueries(
				datastoreInformation,
				"service_by_hcparty_patient_code",
				"service_by_data_owner_patient_code" to DATA_OWNER_PARTITION,
			)
				.startKey(if (descending) to else from)
				.endKey(if (descending) from else to)
				.descending(descending)
				.doNotIncludeDocs()

			idFlows.add(
				client.interleave<ComplexKey, String>(
					viewQueries,
					compareBy(
						{ it.components[0] as? String },
						{ it.components[1] as? String },
						{ it.components[2] as? String },
						{ it.components[3] as? String },
						{ (it.components[4] as? Number)?.toLong() },
					),
					DeduplicationMode.ID_AND_VALUE,
				).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().mapNotNull { it.value },
			)
		}
		emitAll(idFlows.asFlow().flattenConcat().distinct())
	}

	override fun listServiceIdsByDataOwnerPatientDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		patientSecretForeignKeys: List<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	) = flow {
		fun listContactsByDataOwnerPatient() = flow {
			val client = couchDbDispatcher.getClient(datastoreInformation)

			val keys = patientSecretForeignKeys.flatMap { fk ->
				searchKeys.map { key -> arrayOf(key, fk) }
			}

			val viewQueries = createQueries(
				datastoreInformation,
				"by_hcparty_patientfk_openingdate" to MAURICE_PARTITION,
				"by_data_owner_patientfk" to DATA_OWNER_PARTITION,
			).keys(keys).includeDocs()
			emitAll(
				relink(
					client.interleave<Array<String>, Long, Contact>(viewQueries, compareBy({ it[0] }, { it[1] }))
						.filterIsInstance<ViewRowWithDoc<Array<String>, Long, Contact>>().map { it.doc },
				),
			)
		}.distinctById()

		val serviceIdToDate = mutableMapOf<String, Long>()

		listContactsByDataOwnerPatient().collect { contact ->
			contact.services.mapNotNull { service ->
				val date = service.valueDate ?: service.openingDate
				if ((date == null && startDate == null && endDate == null) || date !== null && (startDate == null || date >= startDate) && (endDate == null || date <= endDate)) {
					service.id to (date ?: 0)
				} else {
					null
				}
			}.forEach { (serviceId, date) ->
				val currentDate = serviceIdToDate[serviceId]
				if (currentDate == null || currentDate < date) {
					serviceIdToDate[serviceId] = date
				}
			}
		}

		serviceIdToDate.entries.sortedWith(
			if (descending) {
				Comparator { o1, o2 ->
					o2.value.compareTo(o1.value).let {
						if (it == 0) o2.key.compareTo(o1.key) else it
					}
				}
			} else {
				compareBy({ it.value }, { it.key })
			},
		).forEach { emit(it.key) }
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun listServicesIdsByPatientForeignKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, patientSecretForeignKeys: Set<String>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = patientSecretForeignKeys.flatMap { fk ->
			searchKeys.map { key -> arrayOf(key, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patientfk",
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION,
		).keys(keys).includeDocs()
		emitAll(
			relink(
				client
					.interleave<Array<String>, String, Contact>(viewQueries, compareBy({ it[0] }, { it[1] }))
					.filterIsInstance<ViewRowWithDoc<Array<String>, String, Contact>>().map { it.doc },
			).mapNotNull { c ->
				c.services.map { it.id }.asFlow()
			}.flattenConcat(),
		)
	}

	@View(name = "by_service_emit_modified", map = "classpath:js/contact/By_service_emit_modified.js")
	override fun listIdsByServices(datastoreInformation: IDatastoreInformation, services: Collection<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_service_emit_modified").keys(services).includeDocs(false)
		emitAll(client.queryView<String, ContactIdServiceId>(viewQuery).mapNotNull { it.value })
	}

	override fun listContactsByServices(datastoreInformation: IDatastoreInformation, services: Collection<String>): Flow<Contact> = getContacts(datastoreInformation, this.listIdsByServices(datastoreInformation, services).map { it.contactId })

	override fun relink(cs: Flow<Contact>): Flow<Contact> = cs.map { c ->
		val services = mutableMapOf<String, Service?>()
		c.services.forEach { s -> s.id.let { services[it] = s } }
		c.subContacts.forEach { ss ->
			ss.services.forEach { s ->
				val ssvc = services[s.serviceId]
				// If it is null, leave it null...
				s.service = ssvc
			}
		}
		c
	}

	@View(name = "by_externalid", map = "classpath:js/contact/By_externalid.js")
	override fun findContactsByExternalId(datastoreInformation: IDatastoreInformation, externalId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_externalid")
			.key(externalId)
			.includeDocs(true)

		emitAll(client.queryViewIncludeDocs<String, String, Contact>(viewQuery).mapNotNull { it.doc })
	}

	override fun listContactIdsByExternalId(
		datastoreInformation: IDatastoreInformation,
		externalId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_externalid")
			.key(externalId)
			.includeDocs(false)

		emitAll(client.queryView<String, String>(viewQuery).mapNotNull { it.id })
	}

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Contact' && !doc.deleted && doc._conflicts) emit(doc._id )}")
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "conflicts").includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, Contact>(viewQuery).map { it.doc })
	}

	@View(name = "service_by_data_owner_patient_tag_prefix", map = "classpath:js/contact/Service_by_data_owner_patient_tag_prefix.js", secondaryPartition = BEPPE_PARTITION)
	override fun listServiceIdsByDataOwnerPatientTagCodePrefix(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		patientSecretForeignKeys: Set<String>,
		tagType: String,
		tagCodePrefix: String,
		startValueDate: Long?,
		endValueDate: Long?
	): Flow<String> =
		listServiceIdsByDataOwnerPatientTagOrCodeCodePrefix(
			datastoreInformation,
			searchKeys,
			patientSecretForeignKeys,
			tagType,
			tagCodePrefix,
			startValueDate,
			endValueDate,
			"service_by_data_owner_patient_tag_prefix"
		)

	@View(name = "service_by_data_owner_patient_code_prefix", map = "classpath:js/contact/Service_by_data_owner_patient_code_prefix.js", secondaryPartition = BEPPE_PARTITION)
	override fun listServiceIdsByDataOwnerPatientCodeCodePrefix(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		patientSecretForeignKeys: Set<String>,
		codeType: String,
		codeCodePrefix: String,
		startValueDate: Long?,
		endValueDate: Long?
	): Flow<String> =
		listServiceIdsByDataOwnerPatientTagOrCodeCodePrefix(
			datastoreInformation,
			searchKeys,
			patientSecretForeignKeys,
			codeType,
			codeCodePrefix,
			startValueDate,
			endValueDate,
			"service_by_data_owner_patient_code_prefix"
		)

	private fun listServiceIdsByDataOwnerPatientTagOrCodeCodePrefix(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		patientSecretForeignKeys: Set<String>,
		type: String,
		codePrefix: String,
		startValueDate: Long?,
		endValueDate: Long?,
		view: String
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val allQueries = searchKeys.flatMap { dataOwnerSearchKey ->
			patientSecretForeignKeys.map { patientSecretForeignKey ->
				val from = ComplexKey.of(
					dataOwnerSearchKey,
					patientSecretForeignKey,
					type,
					codePrefix
				)
				val to = ComplexKey.of(
					dataOwnerSearchKey,
					patientSecretForeignKey,
					type,
					codePrefix + "\ufff0"
				)
				val query = createQuery(datastoreInformation, view, BEPPE_PARTITION)
					.startKey(from)
					.endKey(to)
					.includeDocs(false)
				client.queryView<ComplexKey, ServiceIdAndDateValue>(query).let { f ->
					if (startValueDate != null) f.filter { row -> row.value!!.date?.let { it >= startValueDate } == true } else f
				}.let { f ->
					if (endValueDate != null) f.filter { row -> row.value!!.date?.let { it <= endValueDate } == true } else f
				}.map {
					ContactIdMandatoryServiceId(it.id, it.value!!.serviceId)
				}
			}
		}
		emitAll(filterLatestServices(
			client,
			datastoreInformation,
			allQueries.flatMapTo(mutableSetOf()) { it.toList() }
		))
	}

	@View(name = "service_by_data_owner_tag_prefix", map = "classpath:js/contact/Service_by_data_owner_tag_prefix.js", secondaryPartition = BEPPE_PARTITION)
	override fun listServiceIdsByDataOwnerTagCodePrefix(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		tagType: String,
		tagCodePrefix: String,
	): Flow<String> = listServiceIdsByDataOwnerTagOrCodePrefix(
		datastoreInformation,
		searchKeys,
		tagType,
		tagCodePrefix,
		"listServiceIdsByDataOwnerTagCodePrefix",
	)

	@View(name = "service_by_data_owner_code_prefix", map = "classpath:js/contact/Service_by_data_owner_code_prefix.js", secondaryPartition = BEPPE_PARTITION)
	override fun listServiceIdsByDataOwnerCodeCodePrefix(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		codeType: String,
		codeCodePrefix: String,
	): Flow<String> = listServiceIdsByDataOwnerTagOrCodePrefix(
		datastoreInformation,
		searchKeys,
		codeType,
		codeCodePrefix,
		"service_by_data_owner_code_prefix",
	)

	private fun listServiceIdsByDataOwnerTagOrCodePrefix(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		type: String,
		codePrefix: String,
		view: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val allQueries = searchKeys.map { dataOwnerSearchKey ->
			val from = ComplexKey.of(
				dataOwnerSearchKey,
				type,
				codePrefix
			)
			val to = ComplexKey.of(
				dataOwnerSearchKey,
				type,
				codePrefix + "\ufff0"
			)
			val query = createQuery(datastoreInformation, view, BEPPE_PARTITION)
				.startKey(from)
				.endKey(to)
				.includeDocs(false)
			client.queryView<ComplexKey, ServiceIdAndDateValue>(query).map {
				ContactIdMandatoryServiceId(it.id, it.value!!.serviceId)
			}
		}
		emitAll(filterLatestServices(
			client,
			datastoreInformation,
			allQueries.flatMapTo(mutableSetOf()) { it.toList() }
		))
	}

	@View(name = "service_by_data_owner_month_tag_prefix", map = "classpath:js/contact/Service_by_data_owner_month_tag_prefix.js", secondaryPartition = BEPPE_PARTITION)
	override fun listServiceIdsByDataOwnerValueDateMonthTagCodePrefix(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		year: Int?,
		month: Int?,
		tagType: String,
		tagCodePrefix: String,
		startValueDate: Long?,
		endValueDate: Long?
	): Flow<String> = listServiceIdsByDataOwnerValueDateMonthTagOrCodePrefix(
		datastoreInformation,
		searchKeys,
		year,
		month,
		tagType,
		tagCodePrefix,
		startValueDate,
		endValueDate,
		"service_by_data_owner_month_tag_prefix",
	)

	@View(name = "service_by_data_owner_month_code_prefix", map = "classpath:js/contact/Service_by_data_owner_month_code_prefix.js", secondaryPartition = BEPPE_PARTITION)
	override fun listServiceIdsByDataOwnerValueDateMonthCodeCodePrefix(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		year: Int?,
		month: Int?,
		codeType: String,
		codeCodePrefix: String,
		startValueDate: Long?,
		endValueDate: Long?
	): Flow<String> = listServiceIdsByDataOwnerValueDateMonthTagOrCodePrefix(
		datastoreInformation,
		searchKeys,
		year,
		month,
		codeType,
		codeCodePrefix,
		startValueDate,
		endValueDate,
		"service_by_data_owner_month_code_prefix",
	)

	private fun listServiceIdsByDataOwnerValueDateMonthTagOrCodePrefix(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		year: Int?,
		month: Int?,
		type: String,
		codePrefix: String,
		startValueDate: Long?,
		endValueDate: Long?,
		view: String,
	): Flow<String> = flow {
		require((year == null) == (month == null)) { "Year and month must both be non-null or both null" }
		if (startValueDate != null) {
			val parsed = FuzzyDates.getLocalDateTimeWithPrecision(startValueDate, false)?.first
			require(parsed != null) { "startValueDate must be a valid fuzzy date time if provided" }
		}
		if (endValueDate != null) {
			val parsed = FuzzyDates.getLocalDateTimeWithPrecision(endValueDate, false)?.first
			require(parsed != null) { "startValueDate must be a valid fuzzy date time if provided" }
		}
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val allQueries = searchKeys.map { dataOwnerSearchKey ->
			val from = ComplexKey.of(
				year,
				month,
				dataOwnerSearchKey,
				type,
				codePrefix
			)
			val to = ComplexKey.of(
				year,
				month,
				dataOwnerSearchKey,
				type,
				codePrefix + "\ufff0"
			)
			val query = createQuery(datastoreInformation, view, BEPPE_PARTITION)
				.startKey(from)
				.endKey(to)
				.includeDocs(false)
			client.queryView<ComplexKey, ServiceIdAndDateValue>(query).let { f ->
				if (startValueDate != null) f.filter { row -> row.value!!.date?.let { it >= startValueDate } == true } else f
			}.let { f ->
				if (endValueDate != null) f.filter { row -> row.value!!.date?.let { it <= endValueDate } == true } else f
			}.map {
				ContactIdMandatoryServiceId(it.id, it.value!!.serviceId)
			}
		}
		emitAll(filterLatestServices(
			client,
			datastoreInformation,
			allQueries.flatMapTo(mutableSetOf()) { it.toList() }
		))
	}

	@View(name = "by_service_latest", map = "classpath:js/contact/By_service_latest.js", reduce = "classpath:js/contact/By_service_latest_reduce.js", secondaryPartition = BEPPE_PARTITION)
	private fun filterLatestServices(
		client: Client,
		datastoreInformation: IDatastoreInformation,
		services: Collection<ContactIdMandatoryServiceId>
	): Flow<String> = flow {
		val allServiceIds = services.mapTo(mutableSetOf()) { it.serviceId }
		val latestContactForServices = mutableMapOf<String, String>()
		allServiceIds.chunked(1000).forEach { chunk ->
			val query = createQuery(datastoreInformation, "by_service_latest", BEPPE_PARTITION)
				.reduce(true)
				.group(true)
				.keys(chunk)
			client.queryView<String, ComplexKey>(query).collect {
				latestContactForServices[it.key!!] = it.value!!.components[2] as String
			}
		}
		services.forEach {
			if (latestContactForServices[it.serviceId] == it.contactId) emit(it.serviceId)
		}
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when (partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_serviceid" to DATA_OWNER_PARTITION)
			Partitions.Maurice -> warmup(datastoreInformation, "service_by_linked_id" to MAURICE_PARTITION)
			Partitions.Beppe -> warmup(datastoreInformation, "by_service_latest" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}

	@JsonDeserialize(using = ServiceIdAndDateValue.Deserializer::class)
	private data class ServiceIdAndDateValue(
		val serviceId: String,
		val date: Long? // value date with fallback to opening date
	) {
		object Deserializer : JsonDeserializer<ServiceIdAndDateValue>() {
			override fun deserialize(
				p: JsonParser,
				ctxt: DeserializationContext
			): ServiceIdAndDateValue {
				val jsonList = p.readValueAsTree<ArrayNode>().map { it?.let { p.codec.treeToValue(it, Object::class.java) }}
				check(jsonList.size >= 2) { "Expected at least 2 items" }
				return ServiceIdAndDateValue(jsonList[0] as String, (jsonList[1] as Number?)?.toLong())
			}
		}
	}

	private data class ContactIdMandatoryServiceId(val contactId: String, val serviceId: String)
}
