/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.TimeTableDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.TimeTable

@Repository("timeTableDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.TimeTable' && !doc.deleted) emit( null, doc._id )}")
class TimeTableDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<TimeTable>(TimeTable::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), TimeTableDAO {

	@View(name = "by_agenda", map = "classpath:js/timeTable/By_agenda.js")
	override fun listTimeTablesByAgendaId(datastoreInformation: IDatastoreInformation, agendaId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "by_agenda")
			.startKey(agendaId)
			.endKey(agendaId)
			.includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, TimeTable>(viewQuery).map { it.doc })
	}

	override fun listTimeTablesByAgendaIds(datastoreInformation: IDatastoreInformation, agendaIds: Collection<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "by_agenda")
			.keys(agendaIds)
			.includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, TimeTable>(viewQuery).map { it.doc })
	}

	@View(name = "by_agenda_and_startdate", map = "classpath:js/timeTable/By_agenda_and_startdate.js")
	override fun listTimeTablesByStartDateAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from = ComplexKey.of(
			agendaId,
			startDate
		)
		val to = ComplexKey.of(
			agendaId,
			endDate ?: ComplexKey.emptyObject()
		)
		val viewQuery = createQuery(datastoreInformation, "by_agenda_and_startdate")
			.startKey(from)
			.endKey(to)
			.includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<Array<String>, TimeTable>(viewQuery).map { it.doc })
	}

	@View(name = "by_agenda_and_enddate", map = "classpath:js/timeTable/By_agenda_and_enddate.js")
	override fun listTimeTablesByEndDateAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from = ComplexKey.of(
			agendaId,
			startDate
		)
		val to = ComplexKey.of(
			agendaId,
			endDate ?: ComplexKey.emptyObject()
		)
		val viewQuery = createQuery(datastoreInformation, "by_agenda_and_enddate")
			.startKey(from)
			.endKey(to)
			.includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<Array<String>, TimeTable>(viewQuery).map { it.doc })
	}

	override fun listTimeTablesByPeriodAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String): Flow<TimeTable> =
		listTimeTablesByStartDateAndAgendaId(
			datastoreInformation,
			null,
			null,
			agendaId
		).filter { (it.endTime?.let { et -> et > (startDate ?: 0) } ?: true) && (it.startTime?.let { st -> st < (endDate ?: 99999999999999L) } ?: true) }

	override fun listTimeTablesByPeriodAndAgendaIds(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaIds: Collection<String>): Flow<TimeTable> =
		listTimeTablesByAgendaIds(
			datastoreInformation,
			agendaIds
		).filter {
			(it.endTime?.let { et -> et > (startDate ?: 0) } ?: true) && (it.startTime?.let { st -> st < (endDate ?: 99999999999999L) } ?: true)
		}
}
