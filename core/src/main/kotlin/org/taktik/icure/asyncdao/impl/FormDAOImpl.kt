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
import org.taktik.icure.asyncdao.FormDAO
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Form
import org.taktik.icure.utils.distinct
import org.taktik.icure.utils.distinctById
import org.taktik.icure.utils.distinctByIdIf
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.main

@Repository("formDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Form' && !doc.deleted) emit(null, doc._id )}")
internal class FormDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<Form>(Form::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), FormDAO {

	@Deprecated("This method cannot include results with secure delegations, use listFormIdsByDataOwnerPatientOpeningDate instead")
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
			"by_hcparty_patientfk".main(),
		).keys(keys).includeDocs()
		emitAll(client.interleave<Array<String>, String, Form>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, String, Form>>().map { it.doc })
	}.distinctById()

	@View(name = "by_hcparty_patientfk_date", map = "classpath:js/form/By_hcparty_patientfk_date_map.js", secondaryPartition = MAURICE_PARTITION)
	override fun listFormIdsByDataOwnerPatientOpeningDate(
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

	override fun listFormIdsByDataOwnerAndParentId(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		formId: String
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val queries = createQueries(
			datastoreInformation,
			"by_hcparty_parentId",
			"by_data_owner_parentId" to DATA_OWNER_PARTITION
		).keys(searchKeys.map { arrayOf(it, formId) }).doNotIncludeDocs()

		emitAll(client.interleave<Array<String>, String>(
			queries,
			compareBy({it[0]}, {it[1]}),
		).filterIsInstance<ViewRowNoDoc<Array<String>, String>>().map { it.id })
	}.distinct()

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

	@Views(
		View(name = "by_logicalUuid", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Form' && !doc.deleted && doc.logicalUuid) emit( doc.logicalUuid, doc._id )}"),
		View(name = "by_logical_uuid_created", map = "classpath:js/form/By_logical_uuid_created.js", secondaryPartition = MAURICE_PARTITION)
	)
	override fun listFormsByLogicalUuid(datastoreInformation: IDatastoreInformation, formUuid: String, descending: Boolean): Flow<Form> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_logical_uuid_created", MAURICE_PARTITION)
			.startKey(ComplexKey.of(formUuid, if (descending) ComplexKey.emptyObject() else null))
			.endKey(ComplexKey.of(formUuid, if (descending) null else ComplexKey.emptyObject()))
			.descending(descending)
			.includeDocs(true)

		emitAll(client.queryViewIncludeDocs<ComplexKey, String, Form>(viewQuery).map { it.doc })
	}

	override fun listFormIdsByLogicalUuid(
		datastoreInformation: IDatastoreInformation,
		formUuid: String,
		descending: Boolean
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_logical_uuid_created", MAURICE_PARTITION)
			.startKey(ComplexKey.of(formUuid, if (descending) ComplexKey.emptyObject() else null))
			.endKey(ComplexKey.of(formUuid, if (descending) null else ComplexKey.emptyObject()))
			.descending(descending)
			.includeDocs(false)

		emitAll(client.queryView<ComplexKey, String>(viewQuery).map { it.id })
	}

	@Views(
		View(name = "by_uniqueId", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Form' && !doc.deleted && doc.uniqueId) emit( doc.uniqueId, doc._id )}"),
		View(name = "by_unique_id_created", map = "classpath:js/form/By_unique_id_created.js", secondaryPartition = MAURICE_PARTITION)
	)
	override fun listFormsByUniqueId(datastoreInformation: IDatastoreInformation, externalUuid: String, descending: Boolean): Flow<Form> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_unique_id_created", MAURICE_PARTITION)
			.startKey(ComplexKey.of(externalUuid, if (descending) ComplexKey.emptyObject() else null))
			.endKey(ComplexKey.of(externalUuid, if (descending) null else ComplexKey.emptyObject()))
			.descending(descending)
			.includeDocs(true)

		emitAll(client.queryViewIncludeDocs<ComplexKey, String, Form>(viewQuery).map { it.doc })
	}

	override fun listFormIdsByUniqueId(
		datastoreInformation: IDatastoreInformation,
		externalUuid: String,
		descending: Boolean
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_unique_id_created", MAURICE_PARTITION)
			.startKey(ComplexKey.of(externalUuid, if (descending) ComplexKey.emptyObject() else null))
			.endKey(ComplexKey.of(externalUuid, if (descending) null else ComplexKey.emptyObject()))
			.descending(descending)
			.includeDocs(false)

		emitAll(client.queryView<ComplexKey, String>(viewQuery).map { it.id })
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_parentId" to DATA_OWNER_PARTITION)
			Partitions.Maurice -> warmup(datastoreInformation, "by_hcparty_patientfk_date" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}

	}
}
