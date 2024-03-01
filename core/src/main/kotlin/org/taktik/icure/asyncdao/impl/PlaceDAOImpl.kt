/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.PlaceDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Place

@Repository("PlaceDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Place' && !doc.deleted) emit( null, doc._id )}")
class PlaceDAOImpl(
    @Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
    idGenerator: IDGenerator,
    entityCacheFactory: EntityCacheFactory,
    designDocumentProvider: DesignDocumentProvider
) : GenericDAOImpl<Place>(Place::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(Place::class.java), designDocumentProvider), PlaceDAO {
    override fun getAllPlaces(
        datastoreInformation: IDatastoreInformation,
        paginationOffset: PaginationOffset<Nothing>
    ): Flow<ViewQueryResultEvent> = flow {
        val client = couchDbDispatcher.getClient(datastoreInformation)

        val viewQuery = pagedViewQuery(
            datastoreInformation, "all", null, null, paginationOffset, false
        )

        emitAll(client.queryView(viewQuery, Nothing::class.java, String::class.java, Place::class.java))
    }
}
