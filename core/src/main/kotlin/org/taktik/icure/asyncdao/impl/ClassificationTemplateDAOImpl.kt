/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.ClassificationTemplateDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.utils.main
import org.taktik.icure.utils.queryView

@Repository("classificationTemplateDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.ClassificationTemplate' && !doc.deleted) emit( doc.label, doc._id )}",
)
internal class ClassificationTemplateDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider
) : GenericIcureDAOImpl<ClassificationTemplate>(
	entityClass = ClassificationTemplate::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChainLink = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider
),
	ClassificationTemplateDAO {
	override suspend fun getClassificationTemplate(
		datastoreInformation: IDatastoreInformation,
		classificationTemplateId: String,
	): ClassificationTemplate? = get(datastoreInformation, classificationTemplateId)

	override fun findClassificationTemplates(
		datastoreInformation: IDatastoreInformation,
		paginationOffset: PaginationOffset<String>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(
			client = client,
			legacyView = "all".main(),
			configurationView = "all",
			startKey = null,
			endKey = "\ufff0",
			pagination = paginationOffset,
			descending = false
		)
		emitAll(client.queryView<String, String, ClassificationTemplate>(viewQuery))
	}
}
