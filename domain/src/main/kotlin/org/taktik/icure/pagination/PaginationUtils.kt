package org.taktik.icure.pagination

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRow
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.utils.PaginatedDocumentKeyIdPair
import org.taktik.icure.entities.utils.PaginatedList
import java.io.Serializable

/**
 * Converts a [Flow] of [ViewQueryResultEvent] to a [Flow] of [PaginationElement]. Only the first [pageSize] elements
 * of the original flow of [ViewRowWithDoc] type will be converted. The [pageSize] + 1 [ViewRowWithDoc] will be used to
 * extrapolate the [NextPageElement], otherwise no [NextPageElement] will be included in the output flow.
 *
 * @receiver a [Flow] of [ViewQueryResultEvent] which doc type extends [Identifiable] of [String].
 * @param pageSize the number of elements that will be included in the output [Flow].
 * @return a [Flow] of [PaginationElement].
 */
@Suppress("UNCHECKED_CAST")
fun <U : Identifiable<String>> Flow<ViewQueryResultEvent>.toPaginatedFlow(pageSize: Int): Flow<PaginationElement> {
	var emitted = 0
	return transform {
		if (it is ViewRowWithDoc<*, *, *> && (it.doc as? U) != null) {
			when {
				emitted < pageSize -> {
					emitted++
					emit(PaginationRowElement(it.doc as U, it.key))
				}

				emitted == pageSize -> {
					emitted++
					emit(NextPageElement(it.id, it.key))
				}
			}
		}
	}.takeWhile {
		emitted <= pageSize + 1
	}
}

/**
 * Converts a [Flow] of [U] to a [Flow] of [PaginationElement]. Only the first [pageSize] elements
 * of the original flow will be converted. The [pageSize] + 1 element will be used to
 * extrapolate the [NextPageElement], otherwise no [NextPageElement] will be included in the output flow.
 *
 * @receiver a [Flow] of [U].
 * @param pageSize the number of elements that will be included in the output [Flow].
 * @param keyGenerator a function from [U] to [K], where [K] is the type of the paginated list key.
 * @return a [Flow] of [PaginationElement].
 */
fun <U, K> Flow<U>.toPaginatedFlow(pageSize: Int, idGenerator: (U) -> String, keyGenerator: (U) -> K?): Flow<PaginationElement> {
	var emitted = 0
	return transform {
		when {
			emitted < pageSize -> {
				emitted++
				emit(PaginationRowElement(it, keyGenerator(it)))
			}

			emitted == pageSize -> {
				emitted++
				emit(NextPageElement(idGenerator(it), keyGenerator(it)))
			}
		}
	}.takeWhile {
		emitted <= pageSize + 1
	}
}

/**
 * Converts a [Flow] of [ViewQueryResultEvent] to a [Flow] of [PaginationElement] containing only the ids of the entities.
 * Only the first [pageSize] elements of the original flow of [ViewRowNoDoc] type will be converted.
 * The [pageSize] + 1 [ViewRowNoDoc] will be used to extrapolate the [NextPageElement], otherwise no
 * [NextPageElement] will be included in the output flow.
 *
 * @receiver a [Flow] of [ViewQueryResultEvent] which doc type extends [Identifiable] of [String].
 * @param pageSize the number of elements that will be included in the output [Flow].
 * @return a [Flow] of [PaginationElement].
 */
fun Flow<ViewQueryResultEvent>.toPaginatedFlowOfIds(pageSize: Int): Flow<PaginationElement> {
	var emitted = 0
	return transform {
		if (it is ViewRowNoDoc<*, *>) {
			when {
				emitted < pageSize -> {
					emitted++
					emit(PaginationRowElement(it.id, it.key))
				}

				emitted == pageSize -> {
					emitted++
					emit(NextPageElement(it.id, it.key))
				}
			}
		}
	}.takeWhile {
		emitted <= pageSize + 1
	}
}

/**
 * Converts the raw rows of a paginated multi-key ("by keys") view query into a [Flow] of
 * [MultiKeyPaginationElement], turning the [pageSize] + 1-th row into the [MultiKeyPaginationElement.NextPage]
 * cursor instead of returning it as a [MultiKeyPaginationElement.Row]. The receiver is expected to hold at most
 * [pageSize] + 1 rows, which is what the caller should have asked the dao for (see
 * `multiKeyPaginatedViewQuery` in the dao utils for the query side of this).
 *
 * The cursor keeps the key of that extra row first in [keys], dropping all the earlier entries, since it is the
 * only key that was partially returned. Which part of a row identifies its key depends on the view — a search may
 * paginate over one component of a composite view key — so it is up to [keyOfRow] to extract it.
 *
 * A row for which [rowTransform] returns null is **dropped from the page but still counts towards [pageSize]**.
 * That is how a search that deduplicates its rows returns a page shorter than the page size without ever
 * re-querying: one call is one page's worth of rows, and the cursor is still built from the [pageSize] + 1-th raw
 * row, so nothing is skipped across the page boundary.
 *
 * @receiver the raw results of the view query, of which only the [ViewRow]s are considered.
 * @param pageSize the maximum number of rows the page may hold.
 * @param keys the keys the search paginates over, in the order they are visited.
 * @param keyOfRow extracts from a row the key of [keys] it belongs to.
 * @param rowTransform converts a row into the element to return, or null to drop it from the page.
 * @return a [Flow] of [MultiKeyPaginationElement].
 * @throws IllegalStateException if [keyOfRow] returns a key that is not in [keys], which means the wrong part of
 * the row was used to extract it.
 */
fun <T, K> Flow<ViewQueryResultEvent>.toMultiKeyPaginatedFlow(
	pageSize: Int,
	keys: List<K>,
	keyOfRow: (row: ViewRow<*, *, *>) -> K,
	rowTransform: suspend (row: ViewRow<*, *, *>) -> T?,
): Flow<MultiKeyPaginationElement<T, K>> = flow {
	var rowCount = 0
	filterIsInstance<ViewRow<*, *, *>>().collect { row ->
		if (rowCount++ < pageSize) {
			rowTransform(row)?.let { emit(MultiKeyPaginationElement.Row(it)) }
		} else {
			val lastKey = keyOfRow(row)
			val lastKeyIndex = keys.indexOf(lastKey)
			check(lastKeyIndex >= 0) {
				"The key of the last row is not one of the keys the search paginates over, the wrong part of the row was used to extract it."
			}
			emit(MultiKeyPaginationElement.NextPage(row.id, keys.subList(lastKeyIndex, keys.size)))
		}
	}
}

/**
 * Map all the [PaginationRowElement] of a [Flow] of [PaginationElement] from their [SRC] type to a [DST] type.
 * If the flow contains a [NextPageElement] or an [AbortedPageElement], then it will be left unchanged.
 *
 * @receiver a [Flow] of [PaginationElement].
 * @param mapper a function that can convert a [SRC] to a [DST].
 * @return a [Flow] of [PaginationElement].
 * @throws IllegalStateException if there is a [PaginationRowElement] that wraps an element which type is different
 * from [SRC].
 */
//@Suppress("UNCHECKED_CAST")
fun <SRC : Identifiable<String>, DST> Flow<PaginationElement>.mapElements(mapper: suspend (SRC) -> DST): Flow<PaginationElement> = map {
	when (it) {
		is NextPageElement<*>, is AbortedPageElement -> it
		is PaginationRowElement<*, *> -> {
			PaginationRowElement(
				element = mapper(checkNotNull(it.element as? SRC) { "Invalid class in PaginatedElement Flow" }),
				key = it.key,
			)
		}
	}
}

/**
 * Map all the [MultiKeyPaginationElement.Row] of a [Flow] of [MultiKeyPaginationElement] from their [SRC] type to a
 * [DST] type. A [MultiKeyPaginationElement.NextPage] or a [MultiKeyPaginationElement.Aborted] is left unchanged.
 *
 * @receiver a [Flow] of [MultiKeyPaginationElement].
 * @param mapper a function that can convert a [SRC] to a [DST].
 * @return a [Flow] of [MultiKeyPaginationElement].
 */
@JvmName("mapMultiKeyElements")
fun <SRC, DST, K> Flow<MultiKeyPaginationElement<SRC, K>>.mapElements(
	mapper: suspend (SRC) -> DST,
): Flow<MultiKeyPaginationElement<DST, K>> = map {
	when (it) {
		is MultiKeyPaginationElement.NextPage, is MultiKeyPaginationElement.Aborted -> it
		is MultiKeyPaginationElement.Row -> MultiKeyPaginationElement.Row(mapper(it.element))
	}
}

/**
 * Converts a [Flow] of [MultiKeyPaginationElement] to a [Flow] of [PaginationElement], so that a search that paginates
 * over multiple view keys at once is returned in the same shape as every other paginated search: chain
 * [asPaginatedFlux] on the result to serialize it as a normal paginated list.
 *
 * The keys still to visit become the start key of the cursor, since that is what a caller has to pass back to resume,
 * exactly like the start key of a single-key search:
 * - [MultiKeyPaginationElement.Row] becomes a [PaginationRowElement] with no key of its own.
 * - [MultiKeyPaginationElement.NextPage] becomes a [NextPageElement] with [MultiKeyPaginationElement.NextPage.nextKeys]
 *   as its start key and [MultiKeyPaginationElement.NextPage.nextDocId] as its start document id.
 * - [MultiKeyPaginationElement.Aborted] becomes an [AbortedPageElement] with the same error.
 *
 * @receiver a [Flow] of [MultiKeyPaginationElement].
 * @return a [Flow] of [PaginationElement].
 */
fun <T, K> Flow<MultiKeyPaginationElement<T, K>>.asPaginationElements(): Flow<PaginationElement> = map {
	when (it) {
		is MultiKeyPaginationElement.Row -> PaginationRowElement<T, List<K>>(it.element)
		is MultiKeyPaginationElement.NextPage -> NextPageElement(startKeyDocId = it.nextDocId, startKey = it.nextKeys)
		is MultiKeyPaginationElement.Aborted -> AbortedPageElement(it.error)
	}
}

/**
 * Terminal operator for a [Flow] of [PaginationElement]. It collects it generating a [PaginatedList].
 *
 * @receiver a [Flow] of [PaginationElement].
 * @return a [PaginatedList]
 */
@Suppress("UNCHECKED_CAST")
@Deprecated("This method ignores aborted page elements")
suspend fun <T : Serializable, K> Flow<PaginationElement>.toPaginatedList(): PaginatedList<T> {
	var nextKey: NextPageElement<K>? = null
	val rows = mapNotNull {
		when (it) {
			is NextPageElement<*> -> {
				nextKey = it as? NextPageElement<K>
				null
			}
			// The internal PaginatedList has no field to carry it: only the rest layer reports an aborted page.
			is AbortedPageElement -> null
			is PaginationRowElement<*, *> -> it.element as? T
		}
	}.toList()
	return PaginatedList(
		rows = rows,
		nextKeyPair = PaginatedDocumentKeyIdPair(startKey = nextKey?.startKey, startKeyDocId = nextKey?.startKeyDocId),
	)
}

/**
 * Increases the limit of the current [PaginationOffset] by one to take into account the extra element to retrieve as
 * key.
 * @receiver a [PaginationOffset]
 * @return a copy of the [PaginationOffset] with increased limit.
 */
fun <T> PaginationOffset<T>.limitIncludingKey() = copy(limit = limit + 1)
