package org.taktik.icure.pagination

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import org.taktik.couchdb.ViewQueryResultEvent
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
 * Map all the [PaginationRowElement] of a [Flow] of [PaginationElement] from their [SRC] type to a [DST] type.
 * If the flow contains a [NextPageElement], then it will be left unchanged.
 *
 * @receiver a [Flow] of [PaginationElement].
 * @param mapper a function that can convert a [SRC] to a [DST].
 * @return a [Flow] of [PaginationElement].
 * @throws IllegalStateException if there is a [PaginationRowElement] that wraps an element which type is different
 * from [SRC].
 */
@Suppress("UNCHECKED_CAST")
fun <SRC: Identifiable<String>, DST> Flow<PaginationElement>.mapElements(mapper: (SRC) -> DST): Flow<PaginationElement> =
	map {
		when(it) {
			is NextPageElement<*> -> it
			is PaginationRowElement<*, *> -> {
				PaginationRowElement(
					element = mapper(checkNotNull(it.element as? SRC) { "Invalid class in PaginatedElement Flow" }),
					key = it.key
				)
			}
		}
	}

/**
 * Terminal operator for a [Flow] of [PaginationElement]. It collects it generating a [PaginatedList].
 *
 * @receiver a [Flow] of [PaginationElement].
 * @return a [PaginatedList]
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T : Serializable, K> Flow<PaginationElement>.toPaginatedList(): PaginatedList<T> {
	var nextKey: NextPageElement<K>? = null
	val rows = mapNotNull {
		when(it) {
			is NextPageElement<*> -> {
				nextKey = it as? NextPageElement<K>
				null
			}
			is PaginationRowElement<*, *> -> it.element as? T
		}
	}.toList()
	return PaginatedList(
		rows = rows,
		nextKeyPair = PaginatedDocumentKeyIdPair(startKey = nextKey?.startKey, startKeyDocId = nextKey?.startKeyDocId)
	)
}

/**
 * Increases the limit of the current [PaginationOffset] by one to take into account the extra element to retrieve as
 * key.
 * @receiver a [PaginationOffset]
 * @return a copy of the [PaginationOffset] with increased limit.
 */
fun <T> PaginationOffset<T>.limitIncludingKey() = copy(limit = limit + 1)