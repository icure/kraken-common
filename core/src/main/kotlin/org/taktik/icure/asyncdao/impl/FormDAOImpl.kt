/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.*
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
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.FormDAO
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Form
import org.taktik.icure.utils.*

@Repository("formDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Form' && !doc.deleted) emit(null, doc._id )}")
internal class FormDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<Form>(Form::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(Form::class.java), designDocumentProvider, daoConfig = daoConfig), FormDAO {

	@Views(
    	View(name = "by_hcparty_patientfk", map = "classpath:js/form/By_hcparty_patientfk_map.js"),
    	View(name = "by_data_owner_patientfk", map = "classpath:js/form/By_data_owner_patientfk_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listFormsByHcPartyPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { arrayOf(it, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patientfk",
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION
		).keys(keys).includeDocs()
		emitAll(client.interleave<Array<String>, String, Form>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, String, Form>>().map { it.doc })
	}.distinctById()

	override fun listFormsByHcPartyIdPatientSecretKey(
		datastoreInformation: IDatastoreInformation,
		hcPartyId: String,
		secretPatientKey: String,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<ViewQueryResultEvent>  = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val key = ComplexKey.of(hcPartyId, secretPatientKey)
		val viewQueries = createPagedQueries(
			datastoreInformation,
			"by_hcparty_patientfk",
			"by_data_owner_patientfk" to DATA_OWNER_PARTITION,
			key, key, paginationOffset, false
		)
		emitAll(client.interleave<Array<String>, String, Form>(viewQueries, compareBy({it[0]}, {it[1]})))
	}

	@Views(
    	View(name = "by_hcparty_parentId", map = "classpath:js/form/By_hcparty_parent_id.js"),
    	View(name = "by_data_owner_parentId", map = "classpath:js/form/By_data_owner_parent_id.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun listFormsByHcPartyAndParentId(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, formId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.interleave<Array<String>, String, Form>(
			createQueries(datastoreInformation, "by_hcparty_parentId", "by_data_owner_parentId" to DATA_OWNER_PARTITION)
				.keys(searchKeys.map { arrayOf(it, formId) })
				.includeDocs(),
			compareBy({it[0]}, {it[1]}),
		).filterIsInstance<ViewRowWithDoc<Array<String>, String, Form>>().map { it.doc })
	}.distinctByIdIf(searchKeys.size > 1)

	override fun findForms(datastoreInformation: IDatastoreInformation, pagination: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(datastoreInformation, "all", null, null, pagination, false)
		emitAll(client.queryView(viewQuery, Any::class.java, String::class.java, Form::class.java))
	}

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Form' && !doc.deleted && doc._conflicts) emit(doc._id )}")
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryViewIncludeDocsNoValue<String, Form>(createQuery(datastoreInformation, "conflicts").includeDocs(true)).map { it.doc })
	}

	@View(name = "by_logicalUuid", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Form' && !doc.deleted && doc.logicalUuid) emit( doc.logicalUuid, doc._id )}")
	override suspend fun getAllByLogicalUuid(datastoreInformation: IDatastoreInformation, formUuid: String): List<Form> {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_logicalUuid")
			.key(formUuid)
			.includeDocs(true)

		return client.queryViewIncludeDocs<String, String, Form>(viewQuery).map { it.doc /*postLoad(datastoreInformation, it.doc)*/ }.toList().sortedByDescending { it.created ?: 0 }
	}

	@View(name = "by_uniqueId", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Form' && !doc.deleted && doc.uniqueId) emit( doc.uniqueId, doc._id )}")
	override suspend fun getAllByUniqueId(datastoreInformation: IDatastoreInformation, externalUuid: String): List<Form> {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_uniqueId")
			.key(externalUuid)
			.includeDocs(true)

		return client.queryViewIncludeDocs<String, String, Form>(viewQuery).map { it.doc /*postLoad(datastoreInformation, it.doc)*/ }.toList().sortedByDescending { it.created ?: 0 }
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.DataOwner -> {
				val client = couchDbDispatcher.getClient(datastoreInformation)
				client.interleave<Array<String>, String, Form>(
					createQueries(
						datastoreInformation,
						useDataOwnerPartition = true,
						"by_data_owner_parentId" to DATA_OWNER_PARTITION
					).keys(emptyList<String>()).includeDocs(),
					compareBy({it[0]}, {it[1]}),
				).first()
			}
			else -> super.warmupPartition(datastoreInformation, partition)
		}

	}
}
