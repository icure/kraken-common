/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
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
import org.taktik.icure.asyncdao.AccessLogDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.utils.distinct
import org.taktik.icure.utils.interleave

@Repository("accessLogDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.AccessLog' && !doc.deleted) emit( null, doc._id )}")
class AccessLogDAOImpl(
	@Qualifier("patientCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<AccessLog>(AccessLog::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(AccessLog::class.java), designDocumentProvider, daoConfig = daoConfig), AccessLogDAO {

	@View(name = "all_by_date", map = "classpath:js/accesslog/All_by_date_map.js")
	override fun listAccessLogsByDate(datastoreInformation: IDatastoreInformation, fromEpoch: Long, toEpoch: Long, paginationOffset: PaginationOffset<Long>, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"all_by_date",
			fromEpoch,
			toEpoch,
			paginationOffset,
			descending
		)

		emitAll(client.queryView(viewQuery, Long::class.java, String::class.java, AccessLog::class.java))
	}

	@View(name = "all_by_user_date", map = "classpath:js/accesslog/All_by_user_type_and_date_map.js")
	override fun findAccessLogsByUserAfterDate(datastoreInformation: IDatastoreInformation, userId: String, accessType: String?, startDate: Long?, pagination: PaginationOffset<ComplexKey>, descending: Boolean) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(
			userId,
			accessType ?: ComplexKey.emptyObject().takeIf { descending },
			when (descending) {
				true -> ComplexKey.emptyObject()
				false -> startDate
			}
		)
		val endKey = ComplexKey.of(
			userId,
			accessType ?: ComplexKey.emptyObject().takeIf { !descending },
			when (descending) {
				true -> startDate
				false -> ComplexKey.emptyObject()
			}
		)

		val items = client.queryView(
			pagedViewQuery(
				datastoreInformation,
				"all_by_user_date",
				startKey,
				endKey,
				pagination,
				descending
			),
			ComplexKey::class.java,
			String::class.java,
			AccessLog::class.java
		)
		emitAll(items)
	}

	@Views(
		View(name = "by_hcparty_patient", map = "classpath:js/accesslog/By_hcparty_patient_map.js"),
		View(name = "by_data_owner_patient", map = "classpath:js/accesslog/By_data_owner_patient_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findAccessLogsByHCPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { key -> arrayOf(key, fk)}
		}.sortedWith(compareBy({it[0]},{it[1]}))

		val viewQueries = createQueries(
            datastoreInformation,
            "by_hcparty_patient",
            "by_data_owner_patient" to DATA_OWNER_PARTITION
        ).includeDocs()
			.keys(keys)
		emitAll(client.interleave<Array<String>, String, AccessLog>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>,String, AccessLog>>().map { it.doc })
	}.distinct()

	@View(name = "by_hcparty_patient_date", map = "classpath:js/accesslog/By_hcparty_patient_date_map.js", secondaryPartition = MAURICE_PARTITION)
	override fun findAccessLogIdsByDataOwnerPatientDate(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretForeignKeys.flatMap { fk ->
			searchKeys.map { key -> ComplexKey.of(key, fk)}
		}.sortedWith(compareBy({ it.components[0] as String }, { it.components[1] as String }))

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patient_date" to MAURICE_PARTITION,
			"by_data_owner_patient" to DATA_OWNER_PARTITION
		).doNotIncludeDocs().keys(keys)

		client.interleave<ComplexKey, Long>(viewQueries, compareBy({ it.components[0] as String }, { it.components[1] as String }))
			.filterIsInstance<ViewRowNoDoc<ComplexKey, Long>>()
			.mapNotNull {
				if(it.value !== null && (startDate == null || it.value!! >= startDate) && (endDate == null || it.value!! <= endDate)) {
					it.id to it.value!!
				} else null
			}
			.toList()
			.sortedWith(if(descending) Comparator { o1, o2 ->
					o2.second.compareTo(o1.second).let {
						if(it == 0) o2.first.compareTo(o1.first) else it
					}
				} else compareBy({ it.second }, { it.first })
			)
			.forEach { emit(it.first) }
	}.distinctUntilChanged() // This works because ids will be sorted by date first

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_patient" to DATA_OWNER_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
