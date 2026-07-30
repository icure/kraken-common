/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.EmptyObjectKey
import org.taktik.couchdb.entity.NullKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.InsuranceDAO
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.Insurance
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.utils.main

@Repository("insuranceDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Insurance' && !doc.deleted) emit( null, doc._id )}",
)
class InsuranceDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider
) : ConflictDAOImpl<Insurance>(
	entityClass = Insurance::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider
),
	InsuranceDAO {
	@View(name = "all_by_code", map = "classpath:js/insurance/All_by_code_map.js")
	override fun listInsurancesByCode(
		datastoreInformation: IDatastoreInformation,
		code: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(
			client
				.queryViewIncludeDocs<String, String, Insurance>(
					createQuery(
						datastoreInformation = datastoreInformation,
						legacyView = "all_by_code".main(),
						configurationView = "all_by_code"
					).key(code).includeDocs(true),
				).map {
					it.doc
				},
		)
	}

	@View(name = "all_by_name", map = "classpath:js/insurance/All_by_name_map.js")
	override fun listInsurancesByName(
		datastoreInformation: IDatastoreInformation,
		name: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val sanitizedName = sanitizeString(name)

		val ids =
			client
				.queryView<Array<String>, String>(
					createQuery(
						datastoreInformation = datastoreInformation,
						legacyView = "all_by_name".main(),
						configurationView = "all_by_name"
					)
						.startKey(ComplexKey.of(sanitizedName))
						.endKey(
							ComplexKey.of(sanitizedName + "\uFFF0"),
						).includeDocs(false),
				).mapNotNull { it.value }
		emitAll(getEntities(datastoreInformation, ids))
	}

	override fun getAllInsurances(
		datastoreInformation: IDatastoreInformation,
		paginationOffset: PaginationOffset<Nothing>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery =
			createQuery(
				datastoreInformation = datastoreInformation,
				legacyView = "all".main(),
				configurationView = "all"
			)
				.includeDocs(true)
				.reduce(false)
				.startKey(NullKey)
				.startDocId(paginationOffset.startDocumentId)
				.limit(paginationOffset.limit)

		emitAll(client.queryViewIncludeDocs<Any?, String, Insurance>(viewQuery))
	}

	override fun listInsuranceIdsByIdentifiers(datastoreInformation: IDatastoreInformation, identifiers: List<Identifier>): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val queryView = createConfigurationQuery(
			datastoreInformation = datastoreInformation,
			configurationView = "by_identifiers"
		)
			.keys(
				identifiers.map {
					ComplexKey.of(it.system, it.value)
				},
			)

		emitAll(
			client.queryView<ComplexKey, Void>(queryView).map { it.id }
		)
	}

	override fun listInsuranceIdsByCode(datastoreInformation: IDatastoreInformation, codeType: String, codeCode: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(codeType, codeCode)
		val to = ComplexKey.of(codeType, codeCode ?: EmptyObjectKey,)

		val viewQuery = createConfigurationQuery(
			datastoreInformation = datastoreInformation,
			configurationView = "by_codes",
		)
			.startKey(from)
			.endKey(to)
			.includeDocs(false)

		emitAll(client.queryView<ComplexKey, Void>(viewQuery).map { it.id })
	}

	override fun listInsuranceIdsByTag(datastoreInformation: IDatastoreInformation, tagType: String, tagCode: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(tagType, tagCode)
		val to = ComplexKey.of(tagType, tagCode ?: EmptyObjectKey)

		val viewQuery = createConfigurationQuery(
			datastoreInformation = datastoreInformation,
			configurationView = "by_tags",
		)
			.startKey(from)
			.endKey(to)
			.includeDocs(false)

		emitAll(client.queryView<ComplexKey, Void>(viewQuery).map { it.id })
	}

	@View(
		name = "conflicts",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Insurance' && !doc.deleted && doc._conflicts) emit(doc._id) }",
		secondaryPartition = MAURICE_PARTITION
	)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) =
		doListConflicts<Insurance>(datastoreInformation, "conflicts", MAURICE_PARTITION)

	override fun listIdsOfEntitiesWithConflicts(datastoreInformation: IDatastoreInformation): Flow<String> =
		doListIdsOfEntitiesWithConflicts<Insurance>(datastoreInformation, "conflicts", MAURICE_PARTITION)
}
