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
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CalendarItemTypeDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItemType

@Repository("calendarItemTypeDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.CalendarItemType' && !doc.deleted) emit( null, doc._id )}")
class CalendarItemTypeDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider
) : GenericDAOImpl<CalendarItemType>(CalendarItemType::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(CalendarItemType::class.java), designDocumentProvider), CalendarItemTypeDAO {
	@View(name = "all_and_deleted", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.CalendarItemType') emit( doc._id , null )}")
	override fun getCalendarItemsWithDeleted(
		datastoreInformation: IDatastoreInformation,
		offset: PaginationOffset<String>
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = pagedViewQuery(
			datastoreInformation, "all_and_deleted", null, null, offset, false
		)
		emitAll(client.queryView(viewQuery, String::class.java, String::class.java, CalendarItemType::class.java))
	}

	override fun getCalendarItemsWithDeleted(
		datastoreInformation: IDatastoreInformation
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "all_and_deleted").includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<String, CalendarItemType>(viewQuery).map { it.doc })
	}
}
