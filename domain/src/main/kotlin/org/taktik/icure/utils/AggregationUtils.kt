package org.taktik.icure.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlin.math.ceil

suspend fun <T> aggregateResults(
	ids: Collection<String>,
	limit: Int,
	supplier: suspend (Collection<String>) -> Flow<T>,
	filter: suspend (T) -> Boolean = { true },
	entities: List<T> = emptyList(),
	startDocumentId: String? = null,
	heuristic: Int = 2,
) = aggregateResults(ids, limit, supplier, filter, entities, startDocumentId, filteredOutAccumulator = 0, heuristic = heuristic).second

tailrec suspend fun <T, A> aggregateResults(
	ids: Collection<String>,
	limit: Int,
	supplier: suspend (Collection<String>) -> Flow<T>,
	filter: suspend (T) -> Boolean = { true },
	entities: List<T> = emptyList(),
	startDocumentId: String? = null,
	filteredOutAccumulator: A,
	filteredOutElementsReducer: suspend (A, T) -> A = { acc, _ -> acc },
	heuristic: Int = 2,
): Pair<A, List<T>> {
	val heuristicLimit = limit * heuristic

	val sortedIds = (
		startDocumentId?.takeIf { entities.isEmpty() }?.let {
			ids.dropWhile { id -> it != id }
		} ?: ids
		)

	var acc = filteredOutAccumulator
	val filteredEntities = entities +
		supplier(sortedIds.take(heuristicLimit)).filter { el ->
			filter(el).also {
				if (!it) {
					acc = filteredOutElementsReducer(acc, el)
				}
			}
		}.toList()
	val remainingIds = sortedIds.drop(heuristicLimit)

	if (remainingIds.isEmpty() || filteredEntities.count() >= limit) {
		return acc to filteredEntities.take(limit)
	}
	return aggregateResults(
		ids = remainingIds,
		limit = limit,
		supplier = supplier,
		filter = filter,
		entities = filteredEntities,
		filteredOutAccumulator = acc,
		filteredOutElementsReducer = filteredOutElementsReducer,
		heuristic = heuristic,
	)
}

fun <T, S> aggregateResultsAsFlow(
	ids: Collection<S>,
	limit: Int,
	supplier: suspend (Collection<S>) -> Flow<T>,
	filter: suspend (T) -> Boolean = { true },
	startDocumentId: S? = null,
	heuristic: Int = 2,
) = aggregateResultsAsFlow(
	ids = ids,
	limit = limit,
	supplier = supplier,
	filter = filter,
	startDocumentId = startDocumentId,
	heuristic = heuristic.toDouble(),
)

fun <T, S> aggregateResultsAsFlow(
	ids: Collection<S>,
	limit: Int,
	supplier: suspend (Collection<S>) -> Flow<T>,
	filter: suspend (T) -> Boolean = { true },
	startDocumentId: S? = null,
	heuristic: Double = 2.0,
): Flow<T> = flow {
	val heuristicLimit = ceil(limit * heuristic).toInt()
	val sortedIds = startDocumentId?.let { ids.dropWhile { id -> it != id } } ?: ids
	var emitted = 0
	emitAll(
		supplier(sortedIds.take(heuristicLimit)).filter { el ->
			filter(el)
		}.onEach {
			emitted++
		}.onCompletion {
			val remainingIds = sortedIds.drop(heuristicLimit)
			if (remainingIds.isNotEmpty() && emitted < limit) {
				emitAll(
					aggregateResultsAsFlow(
						ids = remainingIds,
						limit = limit - emitted,
						supplier = supplier,
						filter = filter,
						startDocumentId = startDocumentId.takeIf { emitted == 0 },
						heuristic = heuristic,
					),
				)
			}
		},
	)
}
