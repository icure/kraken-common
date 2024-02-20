package org.taktik.icure.pagination

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.utils.PaginatedDocumentKeyIdPair
import org.taktik.icure.entities.utils.PaginatedList
import java.io.Serializable

/**
 * Converts a [Flow] of [ViewQueryResultEvent] to a [Flow] of [PaginatedElement]. Only the first [pageSize] elements
 * of the original flow of [ViewRowWithDoc] type will be converted. The [pageSize] + 1 [ViewRowWithDoc] will be used to
 * extrapolate the [NextPageElement], otherwise no [NextPageElement] will be included in the output flow.
 *
 * @receiver a [Flow] of [ViewQueryResultEvent] which doc type extends [Identifiable] of [String].
 * @param pageSize the number of elements that will be included in the output [Flow].
 * @return a [Flow] of [PaginatedElement].
 */
@Suppress("UNCHECKED_CAST")
fun <U : Identifiable<String>> Flow<ViewQueryResultEvent>.toPaginatedFlow(pageSize: Int): Flow<PaginatedElement> {
	var emitted = 0
	return transform {
		if (it is ViewRowWithDoc<*, *, *> && (it.doc as? U) != null) {
			when {
				emitted < pageSize -> {
					emitted++
					emit(PaginatedRowElement(it.doc as U))
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
 * Map all the [PaginatedRowElement] of a [Flow] of [PaginatedElement] from their [SRC] type to a [DST] type.
 * If the flow contains a [NextPageElement], then it will be left unchanged.
 *
 * @receiver a [Flow] of [PaginatedElement].
 * @param mapper a function that can convert a [SRC] to a [DST].
 * @return a [Flow] of [PaginatedElement].
 * @throws IllegalStateException if there is a [PaginatedRowElement] that wraps an element which type is different
 * from [SRC].
 */
@Suppress("UNCHECKED_CAST")
fun <SRC: Identifiable<String>, DST> Flow<PaginatedElement>.mapElements(mapper: (SRC) -> DST): Flow<PaginatedElement> =
	map {
		when(it) {
			is NextPageElement<*> -> it
			is PaginatedRowElement<*> -> {
				PaginatedRowElement(
					mapper(checkNotNull(it.element as? SRC) { "Invalid class in PaginatedElement Flow" })
				)
			}
		}
	}

/**
 * Terminal operator for a [Flow] of [PaginatedElement]. It collects it generating a [PaginatedList].
 *
 * @receiver a [Flow] of [PaginatedElement].
 * @return a [PaginatedList]
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T : Serializable, K> Flow<PaginatedElement>.toPaginatedList(): PaginatedList<T> {
	var nextKey: NextPageElement<K>? = null
	val rows = mapNotNull {
		when(it) {
			is NextPageElement<*> -> {
				nextKey = it as? NextPageElement<K>
				null
			}
			is PaginatedRowElement<*> -> it.element as? T
		}
	}.toList()
	return PaginatedList(
		rows = rows,
		nextKeyPair = PaginatedDocumentKeyIdPair(startKey = nextKey?.startKey, startKeyDocId = nextKey?.startKeyDocId)
	)
}