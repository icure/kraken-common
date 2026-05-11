/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.icure.asyncdao.AccessLogDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.utils.distinct
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.main
import org.taktik.icure.utils.queryView

@Repository("accessLogDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.AccessLog' && !doc.deleted) emit( null, doc._id )}",
)
class AccessLogDAOImpl(
	@Qualifier("patientCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider
) : ConflictDAOImpl<AccessLog>(
	entityClass = AccessLog::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider
), AccessLogDAO {
	@View(name = "all_by_date", map = "classpath:js/accesslog/All_by_date_map.js")
	override fun listAccessLogsByDate(
		datastoreInformation: IDatastoreInformation,
		fromEpoch: Long,
		toEpoch: Long,
		paginationOffset: PaginationOffset<Long>,
		descending: Boolean,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(
			client = client,
			legacyView = "all_by_date".main(),
			configurationView = "by_date",
			startKey = fromEpoch,
			endKey = toEpoch,
			pagination = paginationOffset,
			descending = descending,
		)
		emitAll(client.queryView(viewQuery, Long::class.java, String::class.java, AccessLog::class.java))
	}

	override fun listAccessLogIdsByDate(
		datastoreInformation: IDatastoreInformation,
		fromEpoch: Long,
		toEpoch: Long,
		descending: Boolean,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(
			client = client,
			legacyView = "all_by_date".main(),
			configurationView = "by_date",
		)
			.startKey(fromEpoch)
			.endKey(toEpoch)
			.includeDocs(false)
			.descending(descending)

		emitAll(client.queryView<String, String>(viewQuery).map { it.id })
	}

	private fun getQueryKeysByUserAfterDate(
		userId: String,
		accessType: String?,
		startDate: Long?,
		descending: Boolean,
	): Pair<ComplexKey, ComplexKey> {
		val startKey =
			ComplexKey.of(
				userId,
				accessType ?: ComplexKey.emptyObject().takeIf { descending },
				when (descending) {
					true -> ComplexKey.emptyObject()
					false -> startDate
				},
			)
		val endKey =
			ComplexKey.of(
				userId,
				accessType ?: ComplexKey.emptyObject().takeIf { !descending },
				when (descending) {
					true -> startDate
					false -> ComplexKey.emptyObject()
				},
			)
		return startKey to endKey
	}

	@View(name = "all_by_user_date", map = "classpath:js/accesslog/All_by_user_type_and_date_map.js")
	override fun findAccessLogsByUserAfterDate(
		datastoreInformation: IDatastoreInformation,
		userId: String,
		accessType: String?,
		startDate: Long?,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val (startKey, endKey) = getQueryKeysByUserAfterDate(userId, accessType, startDate, descending)

		val query = pagedViewQuery(
			client = client,
			legacyView = "all_by_user_date".main(),
			configurationView = "by_user_type_and_date",
			startKey = startKey,
			endKey = endKey,
			pagination = pagination,
			descending = descending,
		)

		val items = client.queryView<ComplexKey, String, AccessLog>(query)
		emitAll(items)
	}

	override fun listAccessLogIdsByUserAfterDate(
		datastoreInformation: IDatastoreInformation,
		userId: String,
		accessType: String?,
		startDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val (startKey, endKey) = getQueryKeysByUserAfterDate(userId, accessType, startDate, descending)

		val viewQuery =
			createQuery(
				client = client,
				legacyView = "all_by_user_date".main(),
				configurationView = "by_user_type_and_date",
			).startKey(startKey)
			.endKey(endKey)
			.includeDocs(false)
			.descending(descending)

		emitAll(client.queryView<ComplexKey, String>(viewQuery).map { it.id })
	}

	@Deprecated("This method is inefficient for high volumes of keys, use listAccessLogIdsByDataOwnerPatientDate instead")
	@Views(
		View(name = "by_hcparty_patient", map = "classpath:js/accesslog/By_hcparty_patient_map.js"),
		View(
			name = "by_data_owner_patient",
			map = "classpath:js/accesslog/By_data_owner_patient_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun findAccessLogsByHCPartyAndSecretPatientKeys(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretPatientKeys: List<String>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys =
			secretPatientKeys
				.flatMap { fk ->
					searchKeys.map { key -> arrayOf(key, fk) }
				}.sortedWith(compareBy({ it[0] }, { it[1] }))

		val viewQueries = createQueries(
				client = client,
				legacyViews = listOf(
					"by_hcparty_patient".main(),
					"by_data_owner_patient" to DATA_OWNER_PARTITION,
				),
				configurationViews = listOf("by_all_delegates_patient")
			).includeDocs().keys(keys)
		emitAll(
			client
				.interleave<Array<String>, String, AccessLog>(viewQueries, compareBy({ it[0] }, { it[1] }))
				.filterIsInstance<ViewRowWithDoc<Array<String>, String, AccessLog>>()
				.map { it.doc },
		)
	}.distinct()

	@View(
		name = "by_hcparty_patient_date",
		map = "classpath:js/accesslog/By_hcparty_patient_date_map.js",
		secondaryPartition = MAURICE_PARTITION,
	)
	override fun listAccessLogIdsByDataOwnerPatientDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = getEntityIdsByDataOwnerPatientDate(
		legacyViews = listOf("by_hcparty_patient_date" to MAURICE_PARTITION, "by_data_owner_patient" to DATA_OWNER_PARTITION),
		configurationViews = listOf("by_all_delegates_patient"),
		datastoreInformation = datastoreInformation,
		searchKeys = searchKeys,
		secretForeignKeys = secretForeignKeys,
		startDate = startDate,
		endDate = endDate,
		descending = descending,
	)

	@View(
		name = "conflicts",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.AccessLog' && !doc.deleted && doc._conflicts) emit(doc._id) }",
		secondaryPartition = MAURICE_PARTITION
	)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) =
		doListConflicts<AccessLog>(datastoreInformation, "conflicts", MAURICE_PARTITION)

	override fun listIdsOfEntitiesWithConflicts(datastoreInformation: IDatastoreInformation): Flow<String> =
		doListIdsOfEntitiesWithConflicts<AccessLog>(datastoreInformation, "conflicts", MAURICE_PARTITION)

	override suspend fun warmupPartition(
		datastoreInformation: IDatastoreInformation,
		partition: Partitions,
	) {
		when (partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_patient" to DATA_OWNER_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
