/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.taktik.couchdb.Client
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRow
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.NullKey
import org.taktik.couchdb.entity.ViewQuery
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.db.PaginationOffset
import java.time.Duration
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

val log: Logger = LoggerFactory.getLogger("org.taktik.icure.utils.DaoUtils")

inline fun <reified K, reified V, reified T> Client.queryView(query: ViewQuery, timeoutDuration: Duration? = null) =
	queryView(query, K::class.java, V::class.java, T::class.java, timeoutDuration)

fun String.main() = this to null

/**
 * The receiver is a list of null terminated queues that are filled and emptied on the basis of the following conventions
 *
 * - An empty queue means no data received yet. It blocks the emission of the events in the other queues
 * - A single element left in a queue means: limit reached. Do not send from *that* queue without loading more elements.
 * - A queue element set to null means: no more element available at all for those keys. Do not block the other queues anymore. Do not use for the next pagination
 *
 * The comparator is used to sort the items and select the queue that is going to be drained. The first queue is the one with the smallest element.
 * The emitter is called with the current item. It returns true if the item has been emitted or discarded (usually because it is a duplicate).
 *
 * @param idx the index of the queue to use
 * @param item the item to add to the queue
 * @param comparator the comparator to use to sort the items
 * @param emitter the emitter to call when an item is selected
 *
 * @return the updated list of queues
 */
suspend fun <T> List<LinkedList<T?>>.pushAndShift(
	idx: Int,
	item: T?,
	comparator: Comparator<T>,
	emitter: suspend (item: T) -> Boolean
): List<LinkedList<T?>> {
	val queue = this[idx]
	queue.add(item)
	return this.also {
		while (

			it.none { queue -> queue.size <= 0 } &&
			this.mapIndexed { index, v -> v.first() to index }
				.sortedWith { (p0, _), (p1, _) ->
					if (p0 == null) 1 else if (p1 == null) -1 else comparator.compare(
						p0,
						p1
					)
				} //Sort so that first element is the one to be emitted, put null last
				.first()
				.let { (selectedItem, idx) ->
					val selectedQueue = this[idx]
					log.debug("Select queue {} with {} elements", idx, selectedQueue.size)
					if (selectedItem != null && selectedQueue.size > 1) {
						emitter(selectedItem).also { hasBeenTreated ->
							if (hasBeenTreated) {
								selectedQueue.removeAt(0)
							}
						}
					} else false//We either have no queue that is not empty or the selected queue only has one element left
				}
		) {
			//Just loop
		}
	}
}


inline fun <reified K, reified V, reified T : Any> Client.interleave(viewQueries: ViewQueries, comparator: Comparator<K>, deduplicationMode: DeduplicationMode = DeduplicationMode.ID, timeoutDuration: Duration? = null ) =
	this.interleave(viewQueries, K::class.java, V::class.java, T::class.java, comparator, deduplicationMode, timeoutDuration)

enum class DeduplicationMode {
	ID, ID_AND_VALUE, DOCUMENT, NONE
}

inline fun <reified K, reified T : Any> Client.interleaveNoValue(viewQueries: ViewQueries, comparator: Comparator<K>, deduplicationMode: DeduplicationMode = DeduplicationMode.ID, timeoutDuration: Duration? = null) =
	this.interleave(viewQueries, K::class.java, Nothing::class.java, T::class.java, comparator, deduplicationMode, timeoutDuration)

inline fun <reified K, reified V : Any> Client.interleave(viewQueries: NoDocViewQueries, comparator: Comparator<K>, deduplicationMode: DeduplicationMode = DeduplicationMode.ID, timeoutDuration: Duration? = null) =
	this.interleave(viewQueries, K::class.java, V::class.java, Nothing::class.java, comparator, deduplicationMode, timeoutDuration)

/**
 * This function is used to interleave the results of multiple view queries.
 * It is useful when you want to paginate over multiple views at the same time.
 * Results are sorted by key and deduplicated on id if deduplicationMode is set to true or if it is null and the viewQueries request for the documents to be included.
 *
 * @param viewQueries the list of view queries to execute
 * @param k the class of the key
 * @param v the class of the value
 * @param t the class of the document
 * @param comparator the comparator to use to sort the keys
 * @param deduplicationMode a [DeduplicationMode] that determines how successive entities will be deduplicated.
 * @param timeoutDuration the timeout duration
 *
 * @return a flow of ViewQueryResultEvent
 */
fun <K, V, T : Any> Client.interleave(viewQueries: List<ViewQuery>, k: Class<K>, v: Class<V>, t: Class<T>, comparator: Comparator<K>, deduplicationMode: DeduplicationMode = DeduplicationMode.ID, timeoutDuration: Duration? = null): Flow<ViewQueryResultEvent> = channelFlow {
	@Suppress("UNCHECKED_CAST")
	if (viewQueries.isNotEmpty()) {
		// Normalisation: just deduplicate and order keys in the same way for all queries
		val normalisedViewQueries = viewQueries.map { it.copy(keys = ((it.keys as List<K>?)?.distinct())?.sortedWith(comparator)) }
		val globalLimit = normalisedViewQueries.first().limit.takeIf { it >= 0 }
		val perQueryLimit = globalLimit?.let { ceil(it.toDouble() / normalisedViewQueries.size).toInt() + 1 } // Limit chosen by a heuristic
		val queues = normalisedViewQueries.map { LinkedList<ViewRow<K,V,T>?>() }
		var sent = 0

		val comparatorSortingIds = Comparator.comparing<ViewRow<K,V,T>, K>({ it.key }, comparator).thenComparing(compareBy { it.id })
		val isDuplicate: (previous: ViewRow<K,V,T>?, current: ViewRow<K,V,T>) -> Boolean  = { previous, current ->
			when(deduplicationMode) {
				DeduplicationMode.ID -> previous?.id == current.id
				DeduplicationMode.ID_AND_VALUE -> (previous?.id == current.id) && ( previous.value == current.value )
				DeduplicationMode.DOCUMENT -> (previous?.id == current.id) && (previous.doc == current.doc)
				// When I reduce + group, I will have empty IDs, no documents and repeated values, so I may want
				// not to deduplicate results.
				DeduplicationMode.NONE -> false
			}
		}

		var previous: ViewRow<K,V,T>? = null
		val sender: suspend (ViewRow<K,V,T>) -> Boolean = { vr ->
			// Note that this will always return true except if the limit is reached
			if (globalLimit == null) { //Always send
				if (!isDuplicate(previous, vr)) {
					send(vr)
					log.debug("No limit: send {} - {}", vr.key, vr.id)
				} else {
					log.debug("No limit: skip {} - {}", vr.key, vr.id)
				}
				previous = vr
				true
			} else if (sent < globalLimit) {
				if (!isDuplicate(previous, vr)) {
					send(vr)
					log.debug("With limit: send {} - {}", vr.key, vr.id)
					sent++
				}
				previous = vr
				true
			} else false
		}

		val mutex = Mutex()
		normalisedViewQueries.mapIndexed { idx, viewQuery ->
			launch {
				val loaded = this@interleave.queryView(
					perQueryLimit?.let { viewQuery.limit(it) } ?: viewQuery, k, v, t, timeoutDuration
				).fold(0) { count, it ->
					@Suppress("UNCHECKED_CAST")
					(it as? ViewRow<K, V, T>)?.also { vr ->
						mutex.withLock { queues.pushAndShift(idx, vr, comparatorSortingIds, sender) }
					}?.let {
						count+1
					}?: count
				}
				if (perQueryLimit == null || loaded < perQueryLimit) {
					// If we get here the query has taken all matching documents already (that means all the documents of all the pages in a paginated context): send termination token null
					mutex.withLock { queues.pushAndShift(idx, null, comparatorSortingIds, sender) } //Flush
				}
			}
		}.forEach { it.join() }

		while (globalLimit != null && globalLimit > sent) {
			//get all queues for which there might be more pages (queues that have been otherwise emptied from there content and for which the termination token has not been added)
			//If several queues have been emptied and are left with one non-null element, we fill first the smallest one as it is the one that blocks the emission of more elements
			val (_, idx) = queues.mapIndexed { idx, it -> it to idx }
				.filter { (it, _) -> it.size == 1 && it.first() != null }
				.sortedWith(Comparator.comparing({ (it, _) -> it.first() }, comparatorSortingIds))
				.firstOrNull() ?: (null to -1)

			if (idx == -1) {
				break
			}
			val latestViewRow = queues[idx].first()!!
			val iterationLimit = min(
				globalLimit - sent + 10 /* better overshoot than do a lot of small queries */,
				perQueryLimit ?: Int.MAX_VALUE
			)

			val selectedViewQuery = normalisedViewQueries[idx]


			val loaded = if (selectedViewQuery.keys != null) {
				this@interleave.queryView(
					selectedViewQuery.limit(iterationLimit).keys(null).startKey(latestViewRow.key).endKey(latestViewRow.key).startDocId(latestViewRow.id),
					k,
					v,
					t,
					timeoutDuration
				).filterIsInstance<ViewRow<K, V, T>>().drop(1).fold(1) { count, it ->
					queues.pushAndShift(idx, it, comparatorSortingIds, sender)
					count + 1
				}.let { count ->
					if (count<iterationLimit) {
						this@interleave.queryView(
							selectedViewQuery.limit(iterationLimit - count).keys((selectedViewQuery.keys as List<K>).filter { k ->
								comparator.compare(latestViewRow.key, k) < 0
							}).startDocId(null),
							k,
							v,
							t,
							timeoutDuration
						).filterIsInstance<ViewRow<K, V, T>>().fold(count) { count2, it ->
							queues.pushAndShift(idx, it, comparatorSortingIds, sender)
							count2 + 1
						}
					} else count
				}
			} else {
				this@interleave.queryView(
					selectedViewQuery.limit(iterationLimit).startKey(latestViewRow.key).startDocId(latestViewRow.id),
					k,
					v,
					t,
					timeoutDuration
				).filterIsInstance<ViewRow<K, V, T>>().drop(1).fold(1) { count, it ->
					(it as? ViewRow<K, V, T>)?.also { vr -> queues.pushAndShift(idx, vr, comparatorSortingIds, sender) }
						?.let {
							count + 1
						} ?: count
				}
			}
			if (loaded < iterationLimit) {
				// If we get here the query has taken all matching documents already (that means all the documents of all the pages in a paginated context): send termination token null
				queues.pushAndShift(idx, null, comparatorSortingIds, sender)
			}
		}
	}
}


suspend fun DesignDocumentProvider.createQueries(client: Client, metadataSource: Any, clazz: Class<*>, viewQueryOnMain: String, viewQueryOnSecondary: Pair<String, String?>, useDataOwner: Boolean) =
	createQueries(client, metadataSource, clazz, useDataOwner, viewQueryOnMain.main(), viewQueryOnSecondary)

suspend fun DesignDocumentProvider.createQueries(client: Client, metadataSource: Any, clazz: Class<*>, useDataOwner: Boolean, vararg viewQueries: Pair<String, String?>) =
	NoDocViewQueries(viewQueries.mapNotNull { (v,p) ->
		if(p == DATA_OWNER_PARTITION && useDataOwner) {
			createQuery(client, metadataSource, v, clazz, p)
		} else null
	})

suspend fun <P> DesignDocumentProvider.createPagedQueries(
	client: Client,
	metadataSource: Any,
	clazz: Class<*>,
	viewQueryOnMain: String,
	viewQueryOnSecondary: Pair<String, String?>,
	startKey: P?,
	endKey: P?,
	pagination: PaginationOffset<P>,
	descending: Boolean,
	useDataOwner: Boolean
) = createPagedQueries(client, metadataSource, clazz, listOf(viewQueryOnMain.main(), viewQueryOnSecondary), startKey, endKey, pagination, descending, useDataOwner)

suspend fun <P> DesignDocumentProvider.createPagedQueries(
	client: Client,
	metadataSource: Any,
	clazz: Class<*>,
	viewQueries: List<Pair<String, String?>>,
	startKey: P?,
	endKey: P?,
	pagination: PaginationOffset<P>,
	descending: Boolean,
	useDataOwner: Boolean
): ViewQueries = ViewQueries(viewQueries.mapNotNull { (v,p) ->
	if(p == DATA_OWNER_PARTITION && useDataOwner) {
		pagedViewQuery(client, metadataSource, v, clazz, startKey, endKey, pagination, descending, p)
	} else null
})

@Suppress("unused")
data class ViewQueries(val queries: List<ViewQuery> = emptyList()) : List<ViewQuery> by queries {
	operator fun plus(viewQueries: ViewQueries): ViewQueries = ViewQueries(queries + viewQueries)

	fun dbPath(dbPath: String) = ViewQueries(queries.map { it.dbPath(dbPath) })
	fun designDocId(designDocId: String) = ViewQueries(queries.map { it.designDocId(designDocId) })
	fun key(key: String?) = ViewQueries(queries.map { it.key(key) })
	fun key(key: Int?) = ViewQueries(queries.map { it.key(key) })
	fun key(key: Long?) = ViewQueries(queries.map { it.key(key) })
	fun key(key: Double?) = ViewQueries(queries.map { it.key(key) })
	fun key(key: Float?) = ViewQueries(queries.map { it.key(key) })
	fun key(key: Boolean?) = ViewQueries(queries.map { it.key(key) })
	fun key(key: Map<String,*>?) = ViewQueries(queries.map { it.key(key) })
	fun key(key: Collection<*>?) = ViewQueries(queries.map { it.key(key) })
	fun key(key: ComplexKey) = ViewQueries(queries.map { it.key(key) })
	fun keys(keys: Collection<*>?) = ViewQueries(queries.map { it.keys(keys?.toList()) })
	fun startKey(startKey: Any?) = ViewQueries(queries.map { it.startKey(startKey) })
	fun endKey(endKey: Any?) = ViewQueries(queries.map { it.endKey(endKey) })
	fun startDocId(startDocId: String?) = ViewQueries(queries.map { it.startDocId(startDocId) })
	fun endDocId(endDocId: String?) = ViewQueries(queries.map { it.endDocId(endDocId) })
	fun limit(limit: Int) = ViewQueries(queries.map { it.limit(limit) })
	fun staleOk(staleOk: Boolean) = ViewQueries(queries.map { it.staleOk(staleOk) })
	fun staleOkUpdateAfter() = ViewQueries(queries.map { it.staleOkUpdateAfter() })
	fun descending(descending: Boolean) = ViewQueries(queries.map { it.descending(descending) })
	fun skip(skip: Int) = ViewQueries(queries.map { it.skip(skip) })
	fun group(value: Boolean) = ViewQueries(queries.map { it.group(value) })
	fun groupLevel(level: Int) = ViewQueries(queries.map { it.groupLevel(level) })
	fun reduce(value: Boolean) = ViewQueries(queries.map { it.reduce(value) })
	fun includeDocs() = ViewQueries(queries.map { it.includeDocs(true) })
	fun doNotIncludeDocs() = NoDocViewQueries(queries.map { it.includeDocs(false) })
	fun inclusiveEnd(value: Boolean) = ViewQueries(queries.map { it.inclusiveEnd(value) })
	fun updateSeq(value: Boolean) = ViewQueries(queries.map { it.updateSeq(value) })
	fun ignoreNotFound(value: Boolean) = ViewQueries(queries.map { it.ignoreNotFound(value) })
}

@Suppress("unused")
data class NoDocViewQueries(val queries: List<ViewQuery> = emptyList()) : List<ViewQuery> by queries {
	operator fun plus(viewQueries: NoDocViewQueries): NoDocViewQueries = NoDocViewQueries(queries + viewQueries)

	fun dbPath(dbPath: String) = NoDocViewQueries(queries.map { it.dbPath(dbPath) })
	fun designDocId(designDocId: String) = NoDocViewQueries(queries.map { it.designDocId(designDocId) })
	fun key(key: String?) = NoDocViewQueries(queries.map { it.key(key) })
	fun key(key: Int?) = NoDocViewQueries(queries.map { it.key(key) })
	fun key(key: Long?) = NoDocViewQueries(queries.map { it.key(key) })
	fun key(key: Double?) = NoDocViewQueries(queries.map { it.key(key) })
	fun key(key: Float?) = NoDocViewQueries(queries.map { it.key(key) })
	fun key(key: Boolean?) = NoDocViewQueries(queries.map { it.key(key) })
	fun key(key: Map<String,*>?) = NoDocViewQueries(queries.map { it.key(key) })
	fun key(key: Collection<*>?) = NoDocViewQueries(queries.map { it.key(key) })
	fun key(key: ComplexKey) = NoDocViewQueries(queries.map { it.key(key) })
	fun keys(keys: Collection<*>?) = NoDocViewQueries(queries.map { it.keys(keys?.toList()) })
	fun startKey(startKey: Any?) = NoDocViewQueries(queries.map { it.startKey(startKey) })
	fun endKey(endKey: Any?) = NoDocViewQueries(queries.map { it.endKey(endKey) })
	fun startDocId(startDocId: String?) = NoDocViewQueries(queries.map { it.startDocId(startDocId) })
	fun endDocId(endDocId: String?) = NoDocViewQueries(queries.map { it.endDocId(endDocId) })
	fun limit(limit: Int) = NoDocViewQueries(queries.map { it.limit(limit) })
	fun staleOk(staleOk: Boolean) = NoDocViewQueries(queries.map { it.staleOk(staleOk) })
	fun staleOkUpdateAfter() = NoDocViewQueries(queries.map { it.staleOkUpdateAfter() })
	fun descending(descending: Boolean) = NoDocViewQueries(queries.map { it.descending(descending) })
	fun skip(skip: Int) = NoDocViewQueries(queries.map { it.skip(skip) })
	fun group(value: Boolean) = NoDocViewQueries(queries.map { it.group(value) })
	fun groupLevel(level: Int) = NoDocViewQueries(queries.map { it.groupLevel(level) })
	fun reduce(value: Boolean) = NoDocViewQueries(queries.map { it.reduce(value) })
	fun includeDocs() = ViewQueries(queries.map { it.includeDocs(true) })
	fun doNotIncludeDocs() = NoDocViewQueries(queries.map { it.includeDocs(false) })
	fun inclusiveEnd(value: Boolean) = NoDocViewQueries(queries.map { it.inclusiveEnd(value) })
	fun updateSeq(value: Boolean) = NoDocViewQueries(queries.map { it.updateSeq(value) })
	fun ignoreNotFound(value: Boolean) = NoDocViewQueries(queries.map { it.ignoreNotFound(value) })
}


inline fun <reified E> PaginationOffset<List<E>>.toComplexKeyPaginationOffset(): PaginationOffset<ComplexKey> =
	PaginationOffset(this.startKey?.toComplexKey(), this.startDocumentId, this.offset, this.limit)

inline fun <reified E> List<E>.toComplexKey(): ComplexKey = ComplexKey.of(*this.toTypedArray())


suspend fun DesignDocumentProvider.createQuery(client: Client, metadataSource: Any, viewName: String, entityClass: Class<*>, secondaryPartition: String? = null): ViewQuery = ViewQuery()
	.designDocId(currentOrAvailableDesignDocumentId(client, entityClass, metadataSource, secondaryPartition))
	.skipIfViewDoesNotExist(secondaryPartition != null)
	.viewName(viewName)

suspend fun <P> DesignDocumentProvider.pagedViewQuery(client: Client, metadataSource: Any, viewName: String, entityClass: Class<*>, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean, secondaryPartition: String? = null): ViewQuery {
	var viewQuery = createQuery(client, metadataSource, viewName, entityClass, secondaryPartition)
		.skipIfViewDoesNotExist(secondaryPartition != null)
		.startKey(pagination.startKey ?: startKey ?: NullKey)
		.includeDocs(true)
		.reduce(false)
		.startDocId(pagination.startDocumentId)
		.limit(pagination.limit)
		.descending(descending)

	if (endKey != null) {
		viewQuery = viewQuery.endKey(endKey)
	}

	return viewQuery
}

suspend fun <P> DesignDocumentProvider.pagedViewQueryOfIds(client: Client, metadataSource: Any, viewName: String, entityClass: Class<*>, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, secondaryPartition: String? = null): ViewQuery {
	var viewQuery = createQuery(client, metadataSource, viewName, entityClass, secondaryPartition)
		.skipIfViewDoesNotExist(secondaryPartition != null)
		.startKey(pagination.startKey ?: startKey ?: NullKey)
		.startDocId(pagination.startDocumentId)
		.includeDocs(false)
		.reduce(false)
		.limit(pagination.limit)

	if (endKey != null) {
		viewQuery = viewQuery.endKey(endKey)
	}

	return viewQuery
}
