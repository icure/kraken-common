/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.ArticleDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.Article

@Repository("articleDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Article' && !doc.deleted) emit( null, doc._id )}")
class ArticleDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<Article>(Article::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(Article::class.java), designDocumentProvider, daoConfig = daoConfig), ArticleDAO
