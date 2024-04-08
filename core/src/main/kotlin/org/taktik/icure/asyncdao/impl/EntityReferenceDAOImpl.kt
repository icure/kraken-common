/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.EntityReferenceDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.EntityReference

@Repository("entityReferenceDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.EntityReference' && !doc.deleted) emit(doc._id, doc._id)}")
class EntityReferenceDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<EntityReference>(EntityReference::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(EntityReference::class.java), designDocumentProvider, daoConfig = daoConfig), EntityReferenceDAO {

	override suspend fun getLatest(datastoreInformation: IDatastoreInformation, prefix: String): EntityReference? {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "all").startKey(prefix + "\ufff0").descending(true).includeDocs(true).limit(1)
		val entityReferences = client.queryViewIncludeDocsNoValue<String, EntityReference>(viewQuery).map { it.doc }

		return entityReferences.firstOrNull()?.takeIf { it.id.startsWith(prefix) }
	}
}
