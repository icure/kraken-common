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
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate

@Repository("classificationTemplateDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.ClassificationTemplate' && !doc.deleted) emit( doc.label, doc._id )}")
internal class ClassificationTemplateDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericIcureDAOImpl<ClassificationTemplate>(ClassificationTemplate::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), ClassificationTemplateDAO {

	override suspend fun getClassificationTemplate(datastoreInformation: IDatastoreInformation, classificationTemplateId: String): ClassificationTemplate? {
		return get(datastoreInformation, classificationTemplateId)
	}

	override fun findClassificationTemplates(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(datastoreInformation, "all", null, "\ufff0", paginationOffset, false)
		emitAll(client.queryView(viewQuery, String::class.java, String::class.java, ClassificationTemplate::class.java))
	}
}
