/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CalendarItemDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.distinctBy
import org.taktik.icure.utils.distinctById
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.interleaveNoValue
import java.time.temporal.ChronoUnit

@Repository("calendarItemDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.CalendarItem' && !doc.deleted) emit( null, doc._id )}")
class CalendarItemDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: EntityCacheFactory,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<CalendarItem>(CalendarItem::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.localOnlyCache(CalendarItem::class.java), designDocumentProvider, daoConfig = daoConfig), CalendarItemDAO {

	@Views(
        View(name = "by_hcparty_and_startdate", map = "classpath:js/calendarItem/By_hcparty_and_startdate.js"),
        View(name = "by_data_owner_and_startdate", map = "classpath:js/calendarItem/By_data_owner_and_startdate.js", secondaryPartition = DATA_OWNER_PARTITION),
    )
	override fun listCalendarItemByStartDateAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(hcPartyId, startDate)
		val to = ComplexKey.of(hcPartyId, endDate ?: ComplexKey.emptyObject())

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_and_startdate",
			"by_data_owner_and_startdate" to DATA_OWNER_PARTITION
		).startKey(from).endKey(to).includeDocs()
		emitAll(client.interleaveNoValue<Array<String>, CalendarItem>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, Nothing, CalendarItem>>().map { it.doc })
	}

	@Views(
        View(name = "by_hcparty_and_enddate", map = "classpath:js/calendarItem/By_hcparty_and_enddate.js"),
        View(name = "by_data_owner_and_enddate", map = "classpath:js/calendarItem/By_data_owner_and_enddate.js", secondaryPartition = DATA_OWNER_PARTITION),
    )
	override fun listCalendarItemByEndDateAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(
			hcPartyId, startDate
		)
		val to = ComplexKey.of(
			hcPartyId, endDate ?: ComplexKey.emptyObject()
		)

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_and_enddate",
			"by_data_owner_and_enddate" to DATA_OWNER_PARTITION
		).startKey(from).endKey(to).includeDocs()
		emitAll(client.interleaveNoValue<Array<String>, CalendarItem>(viewQueries, compareBy({it[0]}, {it[1]}))
			.filterIsInstance<ViewRowWithDoc<Array<String>, Nothing, CalendarItem>>().map { it.doc })
	}

	override fun listCalendarItemByPeriodAndHcPartyId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, hcPartyId: String): Flow<CalendarItem> = flow {
		emitAll(listCalendarItemByStartDateAndHcPartyId(datastoreInformation, startDate, endDate, hcPartyId))
		emitAll(listCalendarItemByEndDateAndHcPartyId(datastoreInformation, startDate, endDate, hcPartyId))
	}.distinctById()

	@View(name = "by_agenda_and_startdate", map = "classpath:js/calendarItem/By_agenda_and_startdate.js")
	override fun listCalendarItemByStartDateAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(
			agendaId, startDate
		)
		val to = ComplexKey.of(
			agendaId, endDate ?: ComplexKey.emptyObject()
		)

		val viewQuery = createQuery(datastoreInformation, "by_agenda_and_startdate").startKey(from).endKey(to).includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<ComplexKey, CalendarItem>(viewQuery).map { it.doc })
	}

	@View(name = "by_agenda_and_enddate", map = "classpath:js/calendarItem/By_agenda_and_enddate.js")
	override fun listCalendarItemByEndDateAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agenda: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(
			agenda, startDate
		)
		val to = ComplexKey.of(
			agenda, endDate ?: ComplexKey.emptyObject()
		)

		val viewQuery = createQuery(datastoreInformation, "by_agenda_and_enddate").startKey(from).endKey(to).includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<ComplexKey, CalendarItem>(viewQuery).map { it.doc })
	}

	override fun listCalendarItemByPeriodAndAgendaId(datastoreInformation: IDatastoreInformation, startDate: Long?, endDate: Long?, agendaId: String) = flow {
		emitAll(listCalendarItemByStartDateAndAgendaId(
			datastoreInformation, startDate?.let {
				/* 1 day in the past to catch long-lasting events that could bracket the search period */
				FuzzyValues.getFuzzyDateTime(FuzzyValues.getDateTime(it)?.minusDays(1) ?: throw IllegalStateException("Failed to compute startDate"), ChronoUnit.SECONDS)
			}, endDate, agendaId
		).filter {
			it.endTime?.let { et -> et > (startDate ?: 0) } ?: true
		})
	}

	@Views(
        View(name = "by_hcparty_patient", map = "classpath:js/calendarItem/By_hcparty_patient_map.js"),
        View(name = "by_data_owner_patient", map = "classpath:js/calendarItem/By_data_owner_patient_map.js", secondaryPartition = DATA_OWNER_PARTITION),
    )
	override fun listCalendarItemsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val keys = secretPatientKeys.flatMap { fk ->
			searchKeys.map { key -> arrayOf(key, fk) }
		}

		val viewQueries = createQueries(
			datastoreInformation,
			"by_hcparty_patient",
			"by_data_owner_patient" to DATA_OWNER_PARTITION
		).keys(keys).includeDocs()
		emitAll(
			client.interleave<Array<String>, String, CalendarItem>(
				viewQueries,
				compareBy({ it[0] }, { it[1] })
			).filterIsInstance<ViewRowWithDoc<Array<String>, String, CalendarItem>>().distinctBy { it.id }.map { it.doc })
	}

	@Views(
		View(name = "by_hcparty_patient_start_time_desc", map = "classpath:js/calendarItem/By_hcparty_patient_start_time_map.js", secondaryPartition = MAURICE_PARTITION),
		View(name = "by_data_owner_patient_start_time_desc", map = "classpath:js/calendarItem/By_data_owner_patient_start_time_map.js", secondaryPartition = DATA_OWNER_PARTITION),
	)
	override fun findCalendarItemsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, hcPartyId: String, secretPatientKey: String, pagination: PaginationOffset<ComplexKey>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = createPagedQueries(
			datastoreInformation,
			listOf("by_hcparty_patient_start_time_desc" to MAURICE_PARTITION, "by_data_owner_patient_start_time_desc" to DATA_OWNER_PARTITION),
			ComplexKey.of(hcPartyId, secretPatientKey, ComplexKey.emptyObject()),
			ComplexKey.of(hcPartyId, secretPatientKey, null),
			pagination,
			true
		)
		emitAll(client.interleave<ComplexKey, String, CalendarItem>(viewQueries,
			compareBy({ it.components[0] as? String }, { it.components[1] as? String }, { (it.components[1] as? Number)?.toLong() })
		))
	}

	override fun findCalendarItemsByHcPartyAndPatient(datastoreInformation: IDatastoreInformation, hcPartyId: String, secretPatientKeys: List<String>, pagination: PaginationOffset<ComplexKey>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val keys = secretPatientKeys.map { fk -> ComplexKey.of(hcPartyId, fk) }

		val constrainedKeys = pagination.startKey?.let {
			keys.indexOf(it).takeIf { idx -> idx >= 0 }?.let { start ->
				keys.subList(start, keys.size)
			} ?: emptyList()
		} ?: keys

		when {
			constrainedKeys.isEmpty() -> {
				//Do nothing
			}

			pagination.startDocumentId == null || constrainedKeys.size == 1 -> {
				emitAll(
					client.interleave<ComplexKey, String, CalendarItem>(
						createQueries(
							datastoreInformation,
							"by_hcparty_patient",
							"by_data_owner_patient" to DATA_OWNER_PARTITION
						)
							.keys(constrainedKeys)
							.startDocId(pagination.startDocumentId)
							.includeDocs()
							.reduce(false)
							.limit(pagination.limit),
						compareBy({ it.components[0] as? String }, { it.components[1] as? String })
					)
				)
			}

			else -> {
				val count = client.interleave<ComplexKey, String, CalendarItem>(
					createQueries(
						datastoreInformation,
						"by_hcparty_patient",
						"by_data_owner_patient" to DATA_OWNER_PARTITION
					)
						.key(constrainedKeys[0])
						.startDocId(pagination.startDocumentId)
						.includeDocs()
						.reduce(false)
						.limit(pagination.limit),
					compareBy({ it.components[0] as? String }, { it.components[1] as? String })
				).onEach { emit(it) }.count { it is ViewRowWithDoc<*,*,*> }

				if (count < pagination.limit) {
					emitAll(
						client.interleave<ComplexKey, String, CalendarItem>(
							createQueries(
								datastoreInformation,
								"by_hcparty_patient",
								"by_data_owner_patient" to DATA_OWNER_PARTITION
							)
								.keys(constrainedKeys.subList(1, constrainedKeys.size))
								.includeDocs()
								.reduce(false)
								.limit(pagination.limit - count),
							compareBy({ it.components[0] as? String }, { it.components[1] as? String })
						)
					)
				}
			}
		}
	}

	@View(name = "by_recurrence_id", map = "classpath:js/calendarItem/by_recurrence_id.js")
	override fun listCalendarItemsByRecurrenceId(datastoreInformation: IDatastoreInformation, recurrenceId: String, offset: PaginationOffset<String>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(
			datastoreInformation, "by_recurrence_id", recurrenceId, recurrenceId, offset, false
		)
		emitAll(client.queryViewIncludeDocsNoValue<String, CalendarItem>(viewQuery))
	}

	override fun listCalendarItemsByRecurrenceId(datastoreInformation: IDatastoreInformation, recurrenceId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "by_recurrence_id").key(recurrenceId).includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, CalendarItem>(viewQuery).map { it.doc })
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_patient_start_time_desc" to DATA_OWNER_PARTITION)
			Partitions.Maurice -> warmup(datastoreInformation, "by_hcparty_patient_start_time_desc" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
