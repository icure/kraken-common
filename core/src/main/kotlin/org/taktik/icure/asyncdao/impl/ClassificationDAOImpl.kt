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
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.ClassificationDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.Classification
import org.taktik.icure.utils.distinctByIdIf
import org.taktik.icure.utils.interleave

@Repository("classificationDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Classification' && !doc.deleted) emit( doc.patientId, doc._id )}")
internal class ClassificationDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericIcureDAOImpl<Classification>(Classification::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), ClassificationDAO {

	override fun listClassificationByPatient(datastoreInformation: IDatastoreInformation, patientId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "all").includeDocs(true).key(patientId)
		emitAll(client.queryViewIncludeDocs<String, String, Classification>(viewQuery).map { it.doc })
	}

	override suspend fun getClassification(datastoreInformation: IDatastoreInformation, classificationId: String): Classification? {
		return get(datastoreInformation, classificationId)
	}

	@Views(
    	View(name = "by_hcparty_patient", map = "classpath:js/classification/By_hcparty_patient_map.js"),
    	View(name = "by_data_owner_patient", map = "classpath:js/classification/By_data_owner_patient_map.js", secondaryPartition = DATA_OWNER_PARTITION)
	)
	override fun listClassificationsByHCPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { key -> ComplexKey.of(key, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patient",
			"by_data_owner_patient" to DATA_OWNER_PARTITION
		).keys(keys).includeDocs()
		emitAll(client.interleave<ComplexKey, String, Classification>(viewQueries, compareBy({it.components[0] as String}, {it.components[1] as String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, Classification>>().map { it.doc }.distinctUntilChangedBy { it.id })

	}.distinctByIdIf(searchKeys.size > 1)

	@View(name = "by_hcparty_patient_date_as_value", map = "classpath:js/classification/By_hcparty_patient_date_as_value_map.js", secondaryPartition = MAURICE_PARTITION)
	override fun listClassificationIdsByDataOwnerPatientCreated(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = getEntityIdsByDataOwnerPatientDate(
		views = listOf("by_hcparty_patient_date_as_value" to MAURICE_PARTITION, "by_data_owner_patient" to DATA_OWNER_PARTITION),
		datastoreInformation = datastoreInformation,
		searchKeys = searchKeys,
		secretForeignKeys = secretForeignKeys,
		startDate = startDate,
		endDate = endDate,
		descending = descending
	)

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_patient" to DATA_OWNER_PARTITION)
			Partitions.Maurice -> warmup(datastoreInformation, "by_hcparty_patient_date_as_value" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}

}
