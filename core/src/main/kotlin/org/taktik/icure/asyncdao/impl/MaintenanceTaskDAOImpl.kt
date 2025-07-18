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
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MaintenanceTaskDAO
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.utils.distinct
import org.taktik.icure.utils.interleave

@Repository("maintenanceTaskDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type === 'org.taktik.icure.entities.MaintenanceTask' && !doc.deleted) emit(null, doc._id)}",
)
class MaintenanceTaskDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
) : GenericIcureDAOImpl<MaintenanceTask>(
	MaintenanceTask::class.java,
	couchDbDispatcher,
	idGenerator,
	entityCacheFactory.getConfiguredCache(),
	designDocumentProvider,
	daoConfig = daoConfig,
),
	MaintenanceTaskDAO {
	@Views(
		View(name = "by_hcparty_identifier", map = "classpath:js/maintenancetask/By_hcparty_identifier_map.js"),
		View(
			name = "by_data_owner_identifier",
			map = "classpath:js/maintenancetask/By_data_owner_identifier_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listMaintenanceTaskIdsByHcPartyAndIdentifier(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		identifiers: List<Identifier>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQueries =
			createQueries(
				datastoreInformation,
				"by_hcparty_identifier",
				"by_data_owner_identifier" to DATA_OWNER_PARTITION,
			).keys(
				identifiers.flatMap {
					searchKeys.map { key -> ComplexKey.of(key, it.system, it.value) }
				},
			).doNotIncludeDocs()
		emitAll(
			client
				.interleave<ComplexKey, String>(
					viewQueries,
					compareBy({
						it.components[0] as? String?
					}, { it.components[1] as? String? }, { it.components[2] as? String? }),
				).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.mapNotNull {
					if (it.key == null || it.key!!.components.size < 3) {
						return@mapNotNull null
					}
					return@mapNotNull it.id
				},
		)
	}.distinct()

	@Views(
		View(name = "by_hcparty_date", map = "classpath:js/maintenancetask/By_hcparty_date_map.js"),
		View(
			name = "by_data_owner_date",
			map = "classpath:js/maintenancetask/By_data_owner_date_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listMaintenanceTaskIdsAfterDate(
		datastoreInformation: IDatastoreInformation,
		healthcarePartyId: String,
		date: Long,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries =
			createQueries(
				datastoreInformation,
				"by_hcparty_date",
				"by_data_owner_date" to DATA_OWNER_PARTITION,
			).startKey(ComplexKey.of(healthcarePartyId, ComplexKey.emptyObject()))
				.endKey(ComplexKey.of(healthcarePartyId, date))
				.descending(true)
				.doNotIncludeDocs()

		emitAll(
			client
				.interleave<ComplexKey, String>(viewQueries, compareBy({ it.components[0] as? String? }, { it.components[1] as? String? }))
				.filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.map { it.id },
		)
	}

	@Views(
		View(name = "by_hcparty_type", map = "classpath:js/maintenancetask/By_hcparty_type_map.js"),
		View(
			name = "by_data_owner_type",
			map = "classpath:js/maintenancetask/By_data_owner_type_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listMaintenanceTaskIdsByHcPartyAndType(
		datastoreInformation: IDatastoreInformation,
		healthcarePartyId: String,
		type: String,
		startDate: Long?,
		endDate: Long?,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries =
			createQueries(
				datastoreInformation,
				"by_hcparty_type",
				"by_data_owner_type" to DATA_OWNER_PARTITION,
			).startKey(
				endDate?.let { ComplexKey.of(healthcarePartyId, type, it) } ?: ComplexKey.of(
					healthcarePartyId,
					type,
					ComplexKey.emptyObject(),
				),
			).endKey(startDate?.let { ComplexKey.of(healthcarePartyId, type, startDate) } ?: ComplexKey.of(healthcarePartyId, type, null))
				.descending(true)
				.doNotIncludeDocs()

		emitAll(
			client
				.interleave<ComplexKey, String>(
					viewQueries,
					compareBy({
						it.components[0] as? String?
					}, { it.components[1] as? String? }, { (it.components[2] as? Number?)?.toLong() }),
				).filterIsInstance<ViewRowNoDoc<ComplexKey, String>>()
				.map { it.id },
		)
	}

	override fun findMaintenanceTasksByIds(
		datastoreInformation: IDatastoreInformation,
		maintenanceTasksId: Flow<String>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(maintenanceTasksId, MaintenanceTask::class.java))
	}

	override suspend fun warmupPartition(
		datastoreInformation: IDatastoreInformation,
		partition: Partitions,
	) {
		when (partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_identifier" to DATA_OWNER_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
