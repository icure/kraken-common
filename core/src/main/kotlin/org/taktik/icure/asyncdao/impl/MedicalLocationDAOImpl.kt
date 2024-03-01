/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.MedicalLocationDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.MedicalLocation

// Differences between lite and cloud version: instantiated as a bean in the respective DAOConfig
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.MedicalLocation' && !doc.deleted) emit( null, doc._id )}")
open class MedicalLocationDAOImpl(
    @Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
    idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider
) : GenericDAOImpl<MedicalLocation>(MedicalLocation::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(MedicalLocation::class.java), designDocumentProvider), MedicalLocationDAO {
	@View(name = "by_post_code", map = "classpath:js/medicallocation/By_post_code_map.js")
	override fun byPostCode(datastoreInformation: IDatastoreInformation, postCode: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewIncludeDocs<String, String, MedicalLocation>(createQuery(
			datastoreInformation,
			"by_post_code"
		).includeDocs(true).key(postCode)).map { it.doc })
	}

	override fun getAllMedicalLocations(
		datastoreInformation: IDatastoreInformation,
		paginationOffset: PaginationOffset<Nothing>
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(
			datastoreInformation, "all", null, null, paginationOffset, false
		)
		emitAll(client.queryView(viewQuery, Nothing::class.java, String::class.java, MedicalLocation::class.java))
	}
}
