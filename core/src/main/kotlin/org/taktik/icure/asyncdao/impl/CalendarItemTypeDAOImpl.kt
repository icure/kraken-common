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
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CalendarItemTypeDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.utils.main
import org.taktik.icure.utils.queryView

@Repository("calendarItemTypeDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.CalendarItemType' && !doc.deleted) emit( null, doc._id )}",
)
class CalendarItemTypeDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider
) : ConflictDAOImpl<CalendarItemType>(
	entityClass = CalendarItemType::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider
), CalendarItemTypeDAO {
	@View(
		name = "by_agenda_id",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.CalendarItemType' && doc.agendaId && !doc.deleted) emit( doc.agendaId , null )}",
		secondaryPartition = MAURICE_PARTITION,
	)
	override fun listCalendarItemTypesByAgendaId(
		datastoreInformation: IDatastoreInformation,
		agendaId: String,
	): Flow<ViewRowWithDoc<String, Nothing, CalendarItemType>> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val query = createQuery(
			datastoreInformation = datastoreInformation,
			legacyView = "by_agenda_id" to MAURICE_PARTITION,
			configurationView = "by_agenda_id"
		).key(agendaId).includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<String, CalendarItemType>(query))
	}

	@View(
		name = "all_and_deleted",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.CalendarItemType') emit( doc._id , null )}",
	)
	override fun getCalendarItemsWithDeleted(
		datastoreInformation: IDatastoreInformation,
		offset: PaginationOffset<String>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = pagedViewQuery(
			datastoreInformation = datastoreInformation,
			legacyView = "all_and_deleted".main(),
			configurationView = "all_and_deleted",
			startKey = null,
			endKey = null,
			pagination = offset,
			descending = false,
		)
		emitAll(client.queryView<String, String, CalendarItemType>(viewQuery))
	}

	override fun getCalendarItemsWithDeleted(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(
			datastoreInformation = datastoreInformation,
			legacyView = "all_and_deleted".main(),
			configurationView = "all_and_deleted"
		).includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<String, CalendarItemType>(viewQuery).map { it.doc })
	}

	@View(
		name = "conflicts",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.CalendarItemType' && !doc.deleted && doc._conflicts) emit(doc._id) }",
		secondaryPartition = MAURICE_PARTITION
	)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) =
		doListConflicts<CalendarItemType>(datastoreInformation, "conflicts", MAURICE_PARTITION)

	override fun listIdsOfEntitiesWithConflicts(datastoreInformation: IDatastoreInformation): Flow<String> =
		doListIdsOfEntitiesWithConflicts<CalendarItemType>(datastoreInformation, "conflicts", MAURICE_PARTITION)
}
