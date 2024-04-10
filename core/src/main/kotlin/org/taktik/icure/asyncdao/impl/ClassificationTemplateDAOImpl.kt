/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.ClassificationTemplateDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.utils.distinctByIdIf
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.subsequentDistinctById

@Repository("classificationTemplateDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.ClassificationTemplate' && !doc.deleted) emit( doc.label, doc._id )}")
internal class ClassificationTemplateDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericIcureDAOImpl<ClassificationTemplate>(ClassificationTemplate::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(ClassificationTemplate::class.java), designDocumentProvider, daoConfig = daoConfig), ClassificationTemplateDAO {

	override suspend fun getClassificationTemplate(datastoreInformation: IDatastoreInformation, classificationTemplateId: String): ClassificationTemplate? {
		return get(datastoreInformation, classificationTemplateId)
	}

	@Views(
    	View(name = "by_hcparty_patient", map = "classpath:js/classificationtemplate/By_hcparty_patient_map.js"),
    	View(name = "by_data_owner_patient", map = "classpath:js/classificationtemplate/By_data_owner_patient_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listClassificationsByHCPartyAndSecretPatientKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val keys = secretPatientKeys.flatMap {
			searchKeys.map { key -> ComplexKey.of(key, it) }
		}

		val viewQueries = createQueries(
            datastoreInformation,
            "by_hcparty_patient",
            "by_data_owner_patient" to DATA_OWNER_PARTITION
        ).keys(keys).includeDocs()
		emitAll(client.interleave<ComplexKey, String, ClassificationTemplate>(viewQueries, compareBy({it.components[0] as String}, {it.components[1] as String}))
			.filterIsInstance<ViewRowWithDoc<ComplexKey, String, ClassificationTemplate>>().map { it.doc }.subsequentDistinctById())
	}.distinctByIdIf(searchKeys.size > 1)

	override fun listClassificationsByHCPartyAndSecretPatientKey(
		datastoreInformation: IDatastoreInformation,
		searchKey: String,
		secretPatientKey: String,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<ViewQueryResultEvent>  = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val key = ComplexKey.of(searchKey, secretPatientKey)

		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_patient",
			"by_data_owner_patient" to DATA_OWNER_PARTITION,
			key, key, paginationOffset, false
		)
		emitAll(client.interleave<ComplexKey, String, ClassificationTemplate>(
			viewQueries,
			compareBy({it.components[0] as String}, {it.components[1] as String}))
		)
	}

	override fun findClassificationTemplates(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(datastoreInformation, "all", null, "\ufff0", paginationOffset, false)
		emitAll(client.queryView(viewQuery, String::class.java, String::class.java, ClassificationTemplate::class.java))
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_patient" to DATA_OWNER_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
