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
			createQuery(datastoreInformation = datastoreInformation, legacyView = "by_agenda_period" to MAURICE_PARTITION, configurationView = "by_agenda_period")
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
				datastoreInformation = datastoreInformation,
				legacyViews = listOf(
					"by_hcparty_and_startdate".main(),
					"by_data_owner_and_startdate" to DATA_OWNER_PARTITION
				),
				configurationView = "by_all_delegates_and_startdate"
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
				datastoreInformation = datastoreInformation,
				legacyViews = listOf(
					"by_hcparty_and_enddate".main(),
					"by_data_owner_and_enddate" to DATA_OWNER_PARTITION
				),
				configurationView = "by_all_delegates_and_enddate"
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
		configurationView: String
	): Set<String> {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val from = ComplexKey.of(dataOwnerId, startDate)
		val to = ComplexKey.of(dataOwnerId, endDate ?: ComplexKey.emptyObject())

		val viewQueries =
			createQueries(datastoreInformation = datastoreInformation, legacyViews = legacyViews, configurationView = configurationView)
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
				configurationView = "by_all_delegates_and_startdate"
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
				configurationView = "by_all_delegates_and_enddate"
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
				datastoreInformation = datastoreInformation,
				legacyView = "by_data_owner_and_last_update" to DATA_OWNER_PARTITION,
				configurationView = "by_all_delegates_and_last_update"
			).startKey(from).endKey(to).descending(descending).includeDocs(false)
		emitAll(
			client
				.queryView<ComplexKey, String>(query)
				.map { it.id },
		)
	}

	/**
	 * Computes the concurrent-occupancy histogram of the calendar items of the agenda [agendaId] over the
	 * period [startDate]..[endDate] (fuzzy date-times, either bound may be null = open).
	 *
	 * It emits a step function as a [Flow] of (fuzzyTimePoint, numberOfBusyCalendarItems) pairs, one per point
	 * in time where the occupancy changes. The count is the number of calendar items active at that instant.
	 *
	 * It does not load the calendar item documents: it reads the `by_agenda_period` view, whose primary row is
	 * keyed at the item start and carries the item end in its value. The value-less day markers that the same
	 * view emits for multi-day items are ignored: the primary rows alone fully describe every interval.
	 *
	 * Calendar items longer than one week are not supported: an item that started more than a week before
	 * [startDate] but is still ongoing within the period will be missed (same limitation as
	 * [collectFrequenciesByPeriodAndHcPartyId]).
	 */
	override fun collectFrequenciesByPeriodAndAgendaId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long?,
		endDate: Long?,
		agendaId: String
	): Flow<Pair<Long, Long>> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		fun fuzzyShiftWeeks(date: Long, weeks: Long): Long =
			FuzzyValues.getDateTime(date)?.plusWeeks(weeks)
				?.let { FuzzyValues.getFuzzyDateTime(it, ChronoUnit.SECONDS) } ?: date

		// Calendar items can't be longer than a week, so any item overlapping the period started at most a week
		// before its start; look back that far to pick up the items already open at the start of the period.
		val lookBack = startDate?.let { fuzzyShiftWeeks(it, -1) }

		val query = createQuery(
			datastoreInformation = datastoreInformation,
			legacyView = "by_agenda_period" to MAURICE_PARTITION,
			configurationView = "by_agenda_period",
		)
			.startKey(ComplexKey.of(agendaId, lookBack))
			.endKey(ComplexKey.of(agendaId, endDate ?: ComplexKey.emptyObject()))
			.includeDocs(false)

		// Reconstruct each item's interval from a single primary row (start = key[1], end = value.e), then turn
		// them into +1 (at start) / -1 (at end) sweep-line events ordered by time (fuzzy Longs sort chronologically).
		val deltas = sortedMapOf<Long, Int>()
		client.queryView<ComplexKey, CalendarItemDAO.CalendarItemStub.BookingDetails?>(query).collect { row ->
			val end = row.value?.endTime ?: return@collect // skip day markers and items with no end time
			val start = (row.key?.components?.getOrNull(1) as? Number)?.toLong() ?: return@collect
			if (end <= start) return@collect
			deltas.merge(start, 1, Int::plus)
			deltas.merge(end, -1, Int::plus)
		}

		var running = 0
		var baselineEmitted = startDate == null
		for ((t, delta) in deltas) {
			if (startDate != null && t < startDate) {
				// Before the period: only build the baseline of items already open at startDate.
				running += delta
				continue
			}
			if (endDate != null && t > endDate) break
			if (!baselineEmitted) {
				// Report the pre-existing occupancy at the start of the period if any item spans into it.
				if (running > 0 && (startDate == null || t > startDate)) emit(startDate!! to running.toLong())
				baselineEmitted = true
			}
			running += delta
			emit(t to running.toLong())
		}
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
			createQuery(
				datastoreInformation = datastoreInformation,
				legacyView = "by_agenda_and_startdate".main(),
				configurationView = "by_agenda_and_startdate"
			)
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

	/**
	 * Computes the concurrent-occupancy histogram of the calendar items of [hcPartyId] over the
	 * period [startDate]..[endDate] (fuzzy date-times, either bound may be null = open).
	 *
	 * It emits a step function as a [Flow] of (fuzzyTimePoint, numberOfBusyCalendarItems) pairs,
	 * one per point in time where the occupancy changes. The count is the number of calendar items
	 * that are active at that instant.
	 *
	 * It does not load the calendar item documents: it reads only the ids and the start/end dates
	 * from the `by_all_delegates_and_startdate` / `by_all_delegates_and_enddate` views, joining the
	 * start and end of each item by its id.
	 *
	 * Calendar items longer than one week are not supported: an item that started more than a week
	 * before [startDate] but is still ongoing within the period will be missed.
	 */
	override fun collectFrequenciesByPeriodAndHcPartyId(
		datastoreInformation: IDatastoreInformation,
		startDate: Long?,
		endDate: Long?,
		hcPartyId: String
	): Flow<Pair<Long, Long>> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		fun fuzzyShiftWeeks(date: Long, weeks: Long): Long =
			FuzzyValues.getDateTime(date)?.plusWeeks(weeks)
				?.let { FuzzyValues.getFuzzyDateTime(it, ChronoUnit.SECONDS) } ?: date

		// Calendar items can't be longer than a week, so any item overlapping the period started at
		// most a week before its start, and ended at most a week after its end.
		val lookBack = startDate?.let { fuzzyShiftWeeks(it, -1) }
		val endUpper = endDate?.let { fuzzyShiftWeeks(it, 1) }

		// Reads (calendarItemId -> fuzzyDate) from one of the date views, without loading documents.
		suspend fun datesById(configurationView: String, upper: Long?): Map<String, Long> {
			val query = createConfigurationQueryOrNull(datastoreInformation, configurationView)
				?.startKey(ComplexKey.of(hcPartyId, lookBack))
				?.endKey(ComplexKey.of(hcPartyId, upper ?: ComplexKey.emptyObject()))
				?.includeDocs(false)
				?: return emptyMap()
			val out = LinkedHashMap<String, Long>()
			client.queryView<ComplexKey, String>(query).collect { row ->
				// An item is emitted once per delegation; keep the first (the date is the same).
				(row.key?.components?.getOrNull(1) as? Number)?.toLong()?.let { out.putIfAbsent(row.id, it) }
			}
			return out
		}

		val startById = datesById("by_all_delegates_and_startdate", endDate)
		val endById = datesById("by_all_delegates_and_enddate", endUpper)

		// Reconstruct each item's interval by joining start and end on the id, then turn them into
		// +1 (at start) / -1 (at end) sweep-line events ordered by time (fuzzy Longs sort chronologically).
		val deltas = sortedMapOf<Long, Int>()
		startById.forEach { (id, start) ->
			val end = endById[id] ?: return@forEach
			if (end <= start) return@forEach
			deltas.merge(start, 1, Int::plus)
			deltas.merge(end, -1, Int::plus)
		}

		var running = 0
		var baselineEmitted = startDate == null
		for ((t, delta) in deltas) {
			if (startDate != null && t < startDate) {
				// Before the period: only build the baseline of items already open at startDate.
				running += delta
				continue
			}
			if (endDate != null && t > endDate) break
			if (!baselineEmitted) {
				// Report the pre-existing occupancy at the start of the period if any item spans into it.
				if (running > 0 && (startDate == null || t > startDate)) emit(startDate!! to running.toLong())
				baselineEmitted = true
			}
			running += delta
			emit(t to running.toLong())
		}
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
				datastoreInformation = datastoreInformation,
				legacyViews = listOf(
					"by_hcparty_patient".main(),
					"by_data_owner_patient" to DATA_OWNER_PARTITION,
				),
				configurationView = "by_all_delegates_patient"
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
				datastoreInformation = datastoreInformation,
				legacyViewQueries = listOf("by_hcparty_patient_start_time".main(), "by_data_owner_patient_start_time_desc" to DATA_OWNER_PARTITION),
				configurationView = "by_all_delegates_patient_start_time",
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
						datastoreInformation = datastoreInformation,
						legacyViews = listOf("by_hcparty_patient_start_time".main(), "by_data_owner_patient_start_time_desc" to DATA_OWNER_PARTITION),
						configurationView = "by_all_delegates_patient_start_time"
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
							datastoreInformation = datastoreInformation,
							legacyViews = listOf("by_hcparty_patient".main(), "by_data_owner_patient" to DATA_OWNER_PARTITION),
							configurationView = "by_all_delegates_patient"
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
								datastoreInformation = datastoreInformation,
								legacyViews = listOf("by_hcparty_patient".main(), "by_data_owner_patient" to DATA_OWNER_PARTITION),
								configurationView = "by_all_delegates_patient"
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
								datastoreInformation = datastoreInformation,
								legacyViews = listOf("by_hcparty_patient".main(), "by_data_owner_patient" to DATA_OWNER_PARTITION),
								configurationView = "by_all_delegates_patient"
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
			datastoreInformation = datastoreInformation,
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
			datastoreInformation = datastoreInformation,
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
			datastoreInformation = datastoreInformation,
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
