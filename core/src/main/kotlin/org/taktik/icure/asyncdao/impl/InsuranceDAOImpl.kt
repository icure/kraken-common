/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

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
import org.taktik.couchdb.entity.NullKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.InsuranceDAO
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.Insurance

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
) : GenericDAOImpl<Insurance>(
	Insurance::class.java,
	couchDbDispatcher,
	idGenerator,
	entityCacheFactory.getConfiguredCache(),
	designDocumentProvider,
	daoConfig = daoConfig,
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
					createQuery(datastoreInformation, "all_by_code").key(code).includeDocs(true),
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
					createQuery(datastoreInformation, "all_by_name")
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
			createQuery(datastoreInformation, "all")
				.includeDocs(true)
				.reduce(false)
				.startKey(NullKey)
				.startDocId(paginationOffset.startDocumentId)
				.limit(paginationOffset.limit)

		emitAll(client.queryViewIncludeDocs<Any?, String, Insurance>(viewQuery))
	}
}
