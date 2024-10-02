/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.utils.distinct
import org.taktik.icure.utils.distinctById
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.main

@Repository("healthElementDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.HealthElement' && !doc.deleted) emit( null, doc._id )}")
internal class HealthElementDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<HealthElement>(HealthElement::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), HealthElementDAO {

	@Views(
		View(name = "by_hcparty", map = "classpath:js/healthelement/By_hcparty_map.js"),
		View(name = "by_data_owner", map = "classpath:js/healthelement/By_data_owner_map.js", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listHealthElementIdsByHcParty(datastoreInformation: IDatastoreInformation, hcPartyId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.interleave<Array<String>, String>(
			createQueries(datastoreInformation, "by_hcparty", "by_data_owner" to DATA_OWNER_PARTITION)
				.startKey(arrayOf(hcPartyId))
				.endKey(arrayOf(hcPartyId))
				.doNotIncludeDocs(),
			compareBy {it[0]}
		).filterIsInstance<ViewRowNoDoc<Array<String>, String>>().mapNotNull { it.id })
	}

	override fun listHealthElementIdsByHcPartyAndSecretPatientKeys(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretPatientKeys: List<String>
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { arrayOf(it, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patient_date" to MAURICE_PARTITION,
			"by_data_owner_patient" to DATA_OWNER_PARTITION
		).keys(keys).doNotIncludeDocs()
		emitAll(client.interleave<Array<String>, Long>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowNoDoc<Array<String>, Long>>().map { it.id })
	}.distinct()

	@Views(
	    View(name = "by_hcparty_and_codes", map = "classpath:js/healthelement/By_hcparty_code_map.js"),
	    View(name = "by_data_owner_and_codes", map = "classpath:js/healthelement/By_data_owner_code_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listHealthElementIdsByHcPartyAndCodes(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, codeType: String, codeCode: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_and_codes",
			"by_data_owner_and_codes" to DATA_OWNER_PARTITION
		)
			.keys(searchKeys.map { arrayOf(it, "$codeType:$codeCode") })
			.doNotIncludeDocs()

		emitAll(client.interleave<Array<String>, String>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowNoDoc<Array<String>, String>>().mapNotNull { it.id })
	}.distinct()

	@Views(
	    View(name = "by_hcparty_and_tags", map = "classpath:js/healthelement/By_hcparty_tag_map.js"),
	    View(name = "by_data_owner_and_tags", map = "classpath:js/healthelement/By_data_owner_tag_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listHealthElementIdsByHcPartyAndTags(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, tagType: String, tagCode: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_and_tags",
			"by_data_owner_and_tags" to DATA_OWNER_PARTITION
		)
			.keys(searchKeys.map { arrayOf(it, "$tagType:$tagCode") })
			.doNotIncludeDocs()

		emitAll(client.interleave<Array<String>, String>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowNoDoc<Array<String>, String>>().mapNotNull { it.id })
	}.distinct()

	@Views(
	    View(name = "by_hcparty_and_status", map = "classpath:js/healthelement/By_hcparty_status_map.js"),
	    View(name = "by_data_owner_and_status", map = "classpath:js/healthelement/By_data_owner_status_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listHealthElementIdsByHcPartyAndStatus(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, status: Int?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_and_status",
			"by_data_owner_and_status" to DATA_OWNER_PARTITION
		)
			.keys(searchKeys.map { ComplexKey.of(it, status) })
			.doNotIncludeDocs()

		emitAll(client.interleave<ComplexKey, String>(viewQueries, compareBy(
			{it.components[0] as? String},
			{(it.components[1] as? Number)?.toLong()}
		))
			.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>().mapNotNull { it.id })
	}.distinct()

	@Views(
	    View(name = "by_hcparty_and_identifiers", map = "classpath:js/healthelement/By_hcparty_identifiers_map.js", secondaryPartition = MAURICE_PARTITION),
	    View(name = "by_data_owner_and_identifiers", map = "classpath:js/healthelement/By_data_owner_identifiers_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listHealthElementsIdsByHcPartyAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_and_identifiers" to MAURICE_PARTITION,
			"by_data_owner_and_identifiers" to DATA_OWNER_PARTITION
		)
			.keys(
				identifiers.flatMap {
					searchKeys.map { key -> ComplexKey.of(key, it.system, it.value) }
				}
			)
			.doNotIncludeDocs()

		emitAll(client.interleave<ComplexKey, String>(viewQueries, compareBy({it.components[0] as? String}, {it.components[1] as? String}, {it.components[2] as? String}))
			.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
			.mapNotNull { it.id })
	}.distinct()

	override suspend fun getHealthElement(datastoreInformation: IDatastoreInformation, healthElementId: String): HealthElement? {
		return get(datastoreInformation, healthElementId)
	}

	@Deprecated("This method cannot include results with secure delegations, use listHealthElementIdsByDataOwnerPatientOpeningDate instead")
	@Views(
		View(name = "by_hcparty_patient", map = "classpath:js/healthelement/By_hcparty_patient_map.js"),
		View(name = "by_hcparty_patient_date", map = "classpath:js/healthelement/By_hcparty_patient_date.js", secondaryPartition = MAURICE_PARTITION),
		View(name = "by_data_owner_patient", map = "classpath:js/healthelement/By_data_owner_patient_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listHealthElementsByHCPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { arrayOf(it, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patient".main(),
		).keys(keys).includeDocs()
		emitAll(client.interleave<Array<String>, String, HealthElement>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, String, HealthElement>>().map { it.doc }.distinctById())
	}

	override fun listHealthElementIdsByDataOwnerPatientOpeningDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = getEntityIdsByDataOwnerPatientDate(
		views = listOf("by_hcparty_patient_date" to MAURICE_PARTITION, "by_data_owner_patient" to DATA_OWNER_PARTITION),
		datastoreInformation = datastoreInformation,
		searchKeys = searchKeys,
		secretForeignKeys = secretForeignKeys,
		startDate = startDate,
		endDate = endDate,
		descending = descending
	)

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.HealthElement' && !doc.deleted && doc._conflicts) emit(doc._id )}")
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocsNoValue<String, HealthElement>(createQuery(datastoreInformation, "conflicts").includeDocs(true)).map { it.doc })
	}

	override fun findHealthElementsByIds(datastoreInformation: IDatastoreInformation, healthElementIds: Flow<String>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(healthElementIds, HealthElement::class.java))
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.Maurice -> warmup(datastoreInformation, "by_hcparty_and_identifiers" to MAURICE_PARTITION)
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_and_identifiers" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}

}
