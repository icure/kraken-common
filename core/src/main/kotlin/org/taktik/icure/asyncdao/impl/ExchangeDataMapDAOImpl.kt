package org.taktik.icure.asyncdao.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.ExchangeDataMapDAO
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.ExchangeDataMap

@Repository("exchangeDataMapDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.ExchangeDataMap' && !doc.deleted) emit(doc._id, doc._id)}")
class ExchangeDataMapDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<ExchangeDataMap>(ExchangeDataMap::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(ExchangeDataMap::class.java), designDocumentProvider, daoConfig = daoConfig), ExchangeDataMapDAO