/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.KeywordDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.Keyword

@Repository("keywordDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Keyword' && !doc.deleted) emit( null, doc._id )}")
internal class KeywordDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericIcureDAOImpl<Keyword>(Keyword::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), KeywordDAO {
	override suspend fun getKeyword(datastoreInformation: IDatastoreInformation, keywordId: String): Keyword? {
		return get(datastoreInformation, keywordId)
	}

	@View(name = "by_user", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Keyword' && !doc.deleted) emit( doc.userId, doc)}")
	override fun getKeywordsByUserId(datastoreInformation: IDatastoreInformation, userId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		emitAll(client.queryView<String, Keyword>(createQuery(datastoreInformation, "by_user").startKey(userId).endKey(userId).includeDocs(false)).mapNotNull { it.value })
	}
}
