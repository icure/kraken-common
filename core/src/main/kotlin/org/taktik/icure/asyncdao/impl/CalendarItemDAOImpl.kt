/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CalendarItemDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.distinctBy
import org.taktik.icure.utils.distinctById
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.interleaveNoValue
import org.taktik.icure.utils.main
import java.time.temporal.ChronoUnit

@Repository("calendarItemDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.CalendarItem' && !doc.deleted) emit( null, doc._id )}",
)
class CalendarItemDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider,
) : ConflictDAOImpl<CalendarItem>(
	entityClass = CalendarItem::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider
),
	CalendarItemDAO {
	@View(name = "by_agenda_period", map = "classpath:js/calendarItem/By_agenda_period.js", secondaryPartition = MAURICE_PARTITION)
	override fun listCalendarItemStubsByAgendaIdAndPeriod(
		datastoreInformation: IDatastoreInformation,
		searchStart: Long,
		searchEnd: Long,
		agendaId: String,
		limit: Int,
		lastKnownDocumentId: String?,
	): Flow<CalendarItemDAO.CalendarItemStub> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from =
			ComplexKey.of(
				agendaId,
				searchStart,
			)
		val to =
			ComplexKey.of(
				agendaId,
				searchEnd,
			)

		val viewQuery =
			createQuery(client = client, legacyView = "by_agenda_period" to MAURICE_PARTITION, configurationView = "by_agenda_period")
				.startKey(from)
				.endKey(to)
				.includeDocs(false)
				.inclusiveEnd(false)
				.startDocId(lastKnownDocumentId)
				.skip(if (lastKnownDocumentId == null) 0 else 1)
				.limit(limit)

		emitAll(
			client.queryView<ComplexKey, CalendarItemDAO.CalendarItemStub.BookingDetails?>(
				viewQuery
			).map {
				CalendarItemDAO.CalendarItemStub(
					it.id,
					it.key!!.components[1] as Long,
					it.value
				)
			}
		)
	}

	@Views(
		View(name = "by_hcparty_and_startdate", map = "classpath:js/calendarItem/By_hcparty_and_startdate.js"),
		View(
			name = "by_data_owner_and_startdate",
			map = "classpath:js/calendarItem/By_data_owner_and_startdate.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listCalendarItemByStartDateAndHcPartyId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long?,
		endDate: Long?,
		hcPartyId: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(hcPartyId, startDate)
		val to = ComplexKey.of(hcPartyId, endDate ?: ComplexKey.emptyObject())

		val viewQueries =
			createQueries(
				client = client,
				legacyViews = listOf(
					"by_hcparty_and_startdate".main(),
					"by_data_owner_and_startdate" to DATA_OWNER_PARTITION
				),
				configurationViews = listOf("by_all_delegates_and_startdate")
			).startKey(from).endKey(to).includeDocs()
		emitAll(
			client
				.interleaveNoValue<Array<String>, CalendarItem>(viewQueries, compareBy({ it[0] }, { it[1] }))
				.filterIsInstance<ViewRowWithDoc<Array<String>, Nothing, CalendarItem>>()
				.map { it.doc },
		)
	}

	@Views(
		View(name = "by_hcparty_and_enddate", map = "classpath:js/calendarItem/By_hcparty_and_enddate.js"),
		View(
			name = "by_data_owner_and_enddate",
			map = "classpath:js/calendarItem/By_data_owner_and_enddate.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listCalendarItemByEndDateAndHcPartyId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long?,
		endDate: Long?,
		hcPartyId: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(hcPartyId, startDate)
		val to = ComplexKey.of(hcPartyId, endDate ?: ComplexKey.emptyObject())

		val viewQueries =
			createQueries(
				client = client,
				legacyViews = listOf(
					"by_hcparty_and_enddate".main(),
					"by_data_owner_and_enddate" to DATA_OWNER_PARTITION
				),
				configurationViews = listOf("by_all_delegates_and_enddate")
			).startKey(from).endKey(to).includeDocs()
		emitAll(
			client
				.interleaveNoValue<Array<String>, CalendarItem>(viewQueries, compareBy({ it[0] }, { it[1] }))
				.filterIsInstance<ViewRowWithDoc<Array<String>, Nothing, CalendarItem>>()
				.map { it.doc },
		)
	}

	suspend fun listCalendarItemIdsByDateAndDataOwnerId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long?,
		endDate: Long?,
		dataOwnerId: String,
		legacyViews: List<Pair<String, String?>>,
		configurationViews: List<String>
	): Set<String> {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(dataOwnerId, startDate)
		val to = ComplexKey.of(dataOwnerId, endDate ?: ComplexKey.emptyObject())

		val viewQueries =
			createQueries(client = client, legacyViews = legacyViews, configurationViews = configurationViews)
				.startKey(from)
				.endKey(to)
				.doNotIncludeDocs()
		return client
			.interleave<Array<String>, CalendarItem>(viewQueries, compareBy({ it[0] }, { it[1] }))
			.filterIsInstance<ViewRowNoDoc<ComplexKey, Nothing>>()
			.map { it.id }
			.toSet(LinkedHashSet()) // More for documentation purposes as it is the default behaviour of .toSet()
	}

	override fun listCalendarItemIdsByPeriodAndDataOwnerId(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
		startDate: Long?,
		endDate: Long?,
	): Flow<String> = flow {
		val idsByStartDate =
			listCalendarItemIdsByDateAndDataOwnerId(
				datastoreInformation = datastoreInformation,
				startDate = startDate,
				endDate = endDate,
				dataOwnerId = dataOwnerId,
				legacyViews = listOf(
					"by_hcparty_and_startdate".main(),
					"by_data_owner_and_startdate" to DATA_OWNER_PARTITION
				),
				configurationViews = listOf("by_all_delegates_and_startdate")
			)
		val idsByEndDate =
			listCalendarItemIdsByDateAndDataOwnerId(
				datastoreInformation = datastoreInformation,
				startDate = startDate,
				endDate = endDate,
				dataOwnerId = dataOwnerId,
				legacyViews = listOf(
					"by_hcparty_and_enddate".main(),
					"by_data_owner_and_enddate" to DATA_OWNER_PARTITION
				),
				configurationViews = listOf("by_all_delegates_and_enddate")
			)
		emitAll(idsByStartDate.union(idsByEndDate).asFlow())
	}

	override fun listCalendarItemByPeriodAndHcPartyId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long?,
		endDate: Long?,
		hcPartyId: String,
	): Flow<CalendarItem> = flow {
		emitAll(listCalendarItemByStartDateAndHcPartyId(datastoreInformation, startDate, endDate, hcPartyId))
		emitAll(listCalendarItemByEndDateAndHcPartyId(datastoreInformation, startDate, endDate, hcPartyId))
	}.distinctById()

	@View(
		name = "by_data_owner_and_last_update",
		map = "classpath:js/calendarItem/By_data_owner_and_last_update.js",
		secondaryPartition = DATA_OWNER_PARTITION,
	)
	override fun listCalendarItemIdsByDataOwnerLifecycleBetween(
		datastoreInformation: IDatastoreInformation,
		searchKey: String,
		startTimestamp: Long?,
		endTimestamp: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from =
			if (descending) {
				ComplexKey.of(
					searchKey,
					endTimestamp ?: ComplexKey.emptyObject(),
				)
			} else {
				ComplexKey.of(searchKey, startTimestamp)
			}
		val to = if (descending) ComplexKey.of(searchKey, startTimestamp) else ComplexKey.of(searchKey, endTimestamp ?: ComplexKey.emptyObject())
		val query =
			createQuery(
				client = client,
				legacyView = "by_data_owner_and_last_update" to DATA_OWNER_PARTITION,
				configurationView = "by_all_delegates_and_last_update"
			).startKey(from).endKey(to).descending(descending).includeDocs(false)
		emitAll(
			client
				.queryView<ComplexKey, String>(query)
				.map { it.id },
		)
	}

	@View(name = "by_agenda_and_startdate", map = "classpath:js/calendarItem/By_agenda_and_startdate.js")
	fun listCalendarItemByStartDateAndAgendaId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long,
		endDate: Long,
		agendaId: String,
		descending: Boolean,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from =
			ComplexKey.of(
				agendaId,
				if (descending) endDate else startDate,
			)
		val to =
			ComplexKey.of(
				agendaId,
				if (descending) startDate else endDate,
			)

		val viewQuery =
			createQuery(client = client, legacyView = "by_agenda_and_startdate".main(), configurationView = "by_agenda_and_startdate")
				.startKey(from)
				.endKey(to)
				.descending(descending)
				.includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<ComplexKey, CalendarItem>(viewQuery).map { it.doc })
	}

	override fun listCalendarItemByPeriodAndAgendaId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long,
		endDate: Long,
		agendaId: String,
		descending: Boolean,
	) = flow {
		emitAll(
			listCalendarItemByStartDateAndAgendaId(
				datastoreInformation,
				startDate.let {
					// 1 day in the past to catch long-lasting events that could bracket the search period
					FuzzyValues.getFuzzyDateTime(
						FuzzyValues.getDateTime(it)?.minusDays(1)
							?: throw IllegalStateException("Failed to compute startDate"),
						ChronoUnit.SECONDS,
					)
				},
				endDate,
				agendaId,
				descending,
			).filter {
				it.endTime?.let { et -> et > startDate } ?: true
			},
		)
	}

	@Views(
		View(name = "by_hcparty_patient", map = "classpath:js/calendarItem/By_hcparty_patient_map.js"),
		View(
			name = "by_data_owner_patient",
			map = "classpath:js/calendarItem/By_data_owner_patient_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun listCalendarItemsByHcPartyAndPatient(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretPatientKeys: List<String>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val keys =
			secretPatientKeys.flatMap { fk ->
				searchKeys.map { key -> arrayOf(key, fk) }
			}

		val viewQueries =
			createQueries(
				client = client,
				legacyViews = listOf(
					"by_hcparty_patient".main(),
					"by_data_owner_patient" to DATA_OWNER_PARTITION,
				),
				configurationViews = listOf("by_all_delegates_patient")
			).keys(keys).includeDocs()
		emitAll(
			client
				.interleave<Array<String>, String, CalendarItem>(
					viewQueries,
					compareBy({ it[0] }, { it[1] }),
				).filterIsInstance<ViewRowWithDoc<Array<String>, String, CalendarItem>>()
				.distinctBy { it.id }
				.map { it.doc },
		)
	}

	@Views(
		View(name = "by_hcparty_patient_start_time", map = "classpath:js/calendarItem/By_hcparty_patient_start_time_map.js"),
		View(
			name = "by_data_owner_patient_start_time_desc",
			map = "classpath:js/calendarItem/By_data_owner_patient_start_time_map.js",
			secondaryPartition = DATA_OWNER_PARTITION,
		),
	)
	override fun findCalendarItemsByHcPartyAndPatient(
		datastoreInformation: IDatastoreInformation,
		hcPartyId: String,
		secretPatientKey: String,
		pagination: PaginationOffset<ComplexKey>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries =
			createPagedQueries(
				client = client,
				legacyViewQueries = listOf("by_hcparty_patient_start_time".main(), "by_data_owner_patient_start_time_desc" to DATA_OWNER_PARTITION),
				configurationViews = listOf("by_all_delegates_patient_start_time"),
				startKey = ComplexKey.of(hcPartyId, secretPatientKey, ComplexKey.emptyObject()),
				endKey = ComplexKey.of(hcPartyId, secretPatientKey, null),
				pagination = pagination,
				descending = true,
			)
		emitAll(
			client.interleave<ComplexKey, String, CalendarItem>(
				viewQueries,
				compareBy({ it.components[0] as? String }, { it.components[1] as? String }, { (it.components[2] as? Number)?.toLong() }),
			),
		)
	}

	// Here I cannot use the function in the generic DAO because the date is in the key, not in the value.
	@OptIn(ExperimentalCoroutinesApi::class)
	override fun listCalendarItemIdsByDataOwnerPatientStartTime(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val queries =
			searchKeys
				.flatMap { searchKey ->
					secretForeignKeys.map { fk ->
						Pair(
							ComplexKey.of(searchKey, fk, startDate),
							ComplexKey.of(searchKey, fk, endDate ?: ComplexKey.emptyObject()),
						)
					}
				}.flatMap {
					createQueries(
						client = client,
						legacyViews = listOf("by_hcparty_patient_start_time".main(), "by_data_owner_patient_start_time_desc" to DATA_OWNER_PARTITION),
						configurationViews = listOf("by_all_delegates_patient_start_time")
					).startKey(it.first)
						.endKey(it.second)
						.doNotIncludeDocs()
						.queries
				}

		queries
			.asFlow()
			.flatMapConcat { q -> client.queryView<ComplexKey, Long>(q) }
			.mapNotNull {
				it.key?.let { key ->
					val startTime = key.components[2] as? Long
					if ((startTime == null && startDate == null && endDate == null) ||
						startTime !== null &&
						(startDate == null || startTime >= startDate) &&
						(endDate == null || startTime <= endDate)
					) {
						it.id to (startTime ?: 0)
					} else {
						null
					}
				}
			}.toList()
			.sortedWith(
				if (descending) {
					Comparator { o1, o2 ->
						o2.second.compareTo(o1.second).let {
							if (it == 0) o2.first.compareTo(o1.first) else it
						}
					}
				} else {
					compareBy({ it.second }, { it.first })
				},
			).forEach { emit(it.first) }
	}.distinctUntilChanged() // This works because ids will be sorted by start time first

	override fun findCalendarItemsByHcPartyAndPatient(
		datastoreInformation: IDatastoreInformation,
		hcPartyId: String,
		secretPatientKeys: List<String>,
		pagination: PaginationOffset<ComplexKey>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val keys = secretPatientKeys.map { fk -> ComplexKey.of(hcPartyId, fk) }

		val constrainedKeys =
			pagination.startKey?.let {
				keys.indexOf(it).takeIf { idx -> idx >= 0 }?.let { start ->
					keys.subList(start, keys.size)
				} ?: emptyList()
			} ?: keys

		when {
			constrainedKeys.isEmpty() -> {
				// Do nothing
			}

			pagination.startDocumentId == null || constrainedKeys.size == 1 -> {
				emitAll(
					client.interleave<ComplexKey, String, CalendarItem>(
						createQueries(
							client = client,
							legacyViews = listOf("by_hcparty_patient".main(), "by_data_owner_patient" to DATA_OWNER_PARTITION),
							configurationViews = listOf("by_all_delegates_patient")
						).keys(constrainedKeys)
							.startDocId(pagination.startDocumentId)
							.includeDocs()
							.reduce(false)
							.limit(pagination.limit),
						compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
					),
				)
			}

			else -> {
				val count =
					client
						.interleave<ComplexKey, String, CalendarItem>(
							createQueries(
								client = client,
								legacyViews = listOf("by_hcparty_patient".main(), "by_data_owner_patient" to DATA_OWNER_PARTITION),
								configurationViews = listOf("by_all_delegates_patient")
							).key(constrainedKeys[0])
								.startDocId(pagination.startDocumentId)
								.includeDocs()
								.reduce(false)
								.limit(pagination.limit),
							compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
						).onEach { emit(it) }
						.count { it is ViewRowWithDoc<*, *, *> }

				if (count < pagination.limit) {
					emitAll(
						client.interleave<ComplexKey, String, CalendarItem>(
							createQueries(
								client = client,
								legacyViews = listOf("by_hcparty_patient".main(), "by_data_owner_patient" to DATA_OWNER_PARTITION),
								configurationViews = listOf("by_all_delegates_patient")
							).keys(constrainedKeys.subList(1, constrainedKeys.size))
								.includeDocs()
								.reduce(false)
								.limit(pagination.limit - count),
							compareBy({ it.components[0] as? String }, { it.components[1] as? String }),
						),
					)
				}
			}
		}
	}

	@View(name = "by_recurrence_id", map = "classpath:js/calendarItem/by_recurrence_id.js")
	override fun listCalendarItemsByRecurrenceId(
		datastoreInformation: IDatastoreInformation,
		recurrenceId: String,
		offset: PaginationOffset<String>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = pagedViewQuery(
			client = client,
			legacyView = "by_recurrence_id".main(),
			configurationView = "by_recurrence_id",
			startKey = recurrenceId,
			endKey = recurrenceId,
			pagination = offset,
			descending = false,
		)
		emitAll(client.queryViewIncludeDocsNoValue<String, CalendarItem>(viewQuery))
	}

	override fun listCalendarItemsByRecurrenceId(
		datastoreInformation: IDatastoreInformation,
		recurrenceId: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(
			client = client,
			legacyView = "by_recurrence_id".main(),
			configurationView = "by_recurrence_id"
		).key(recurrenceId).includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, CalendarItem>(viewQuery).map { it.doc })
	}

	override fun listCalendarItemIdsByRecurrenceId(
		datastoreInformation: IDatastoreInformation,
		recurrenceId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(
			client = client,
			legacyView = "by_recurrence_id".main(),
			configurationView = "by_recurrence_id"
		).key(recurrenceId).includeDocs(false)
		emitAll(client.queryView<String, String>(viewQuery).map { it.id })
	}

	@View(
		name = "conflicts",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.CalendarItem' && !doc.deleted && doc._conflicts) emit(doc._id) }",
		secondaryPartition = MAURICE_PARTITION
	)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) =
		doListConflicts<CalendarItem>(datastoreInformation, "conflicts", MAURICE_PARTITION)

	override fun listIdsOfEntitiesWithConflicts(datastoreInformation: IDatastoreInformation): Flow<String> =
		doListIdsOfEntitiesWithConflicts<CalendarItem>(datastoreInformation, "conflicts", MAURICE_PARTITION)

	override suspend fun warmupPartition(
		datastoreInformation: IDatastoreInformation,
		partition: Partitions,
	) {
		when (partition) {
			Partitions.DataOwner -> warmup(datastoreInformation, "by_data_owner_patient_start_time_desc" to DATA_OWNER_PARTITION)
			Partitions.Maurice -> warmup(datastoreInformation, "by_agenda_period" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
