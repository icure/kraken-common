/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.AgendaDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.utils.entities.embed.iterator
import org.taktik.icure.utils.sortedMerge
import java.time.Duration

@Repository("AgendaDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Agenda' && !doc.deleted) emit( null, doc._id )}")
class AgendaDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<Agenda>(Agenda::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), AgendaDAO {

	@View(name = "by_user", map = "classpath:js/agenda/By_user.js")
	override fun getAgendasByUser(datastoreInformation: IDatastoreInformation, userId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_user")
			.startKey(userId)
			.endKey(userId)
			.includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<String, Agenda>(viewQuery).map { it.doc })
	}

	override fun listAgendaIdsByUser(datastoreInformation: IDatastoreInformation, userId: String): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_user")
			.startKey(userId)
			.endKey(userId)
			.includeDocs(false)

		emitAll(client.queryView<String, String>(viewQuery).mapNotNull { it.id })
	}

	@View(name = "readable_by_user", map = "classpath:js/agenda/Readable_by_user.js")
	override fun getReadableAgendaByUser(datastoreInformation: IDatastoreInformation, userId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "readable_by_user")
			.startKey(userId)
			.endKey(userId)
			.includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<String, Agenda>(viewQuery).map { it.doc })
	}

	override fun listReadableAgendaIdsByUser(
		datastoreInformation: IDatastoreInformation,
		userId: String
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "readable_by_user")
			.startKey(userId)
			.endKey(userId)
			.includeDocs(false)

		emitAll(client.queryView<String, String>(viewQuery).map { it.id })
	}

	@View(name = "by_calendar_item_type", map = "classpath:js/agenda/By_calendar_item_type.js")
	override fun listAgendasWithAvailabilitiesOfType(
		datastoreInformation: IDatastoreInformation,
		calendarItemType: CalendarItemType,
		startDate: Long,
		endDate: Long
	): Flow<Agenda> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "by_calendar_item_type")
			.key(calendarItemType.id)
			.includeDocs(true)

		val startDateToTheMinute = startDate % 100
		val endDateToTheMinute = endDate % 100
		val duration = Duration.ofMinutes(calendarItemType.duration.toLong())

		emitAll(
			client.queryViewIncludeDocs<String, Void, Agenda>(viewQuery).map {
				it.doc
			}.filter { agenda ->
				val ttis = agenda.timeTables.filter {
					(it.startTime ?: 0) <= endDateToTheMinute && (it.endTime ?: Long.MAX_VALUE) >= startDateToTheMinute
				}.flatMap { tt ->
					tt.items.filter { tti -> tti.calendarItemTypeId == calendarItemType.id }.map { it to tt }
				}
				val iterator = ttis.map { (tti, tt) -> tti.iterator(startDateToTheMinute.coerceAtLeast(tt.startTime ?: 0), endDateToTheMinute.coerceAtMost(tt.endTime ?: 0), duration) }.sortedMerge()
				iterator.hasNext()
			}
		)
	}
}
