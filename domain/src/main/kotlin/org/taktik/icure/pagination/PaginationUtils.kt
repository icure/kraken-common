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

@Suppress("UNCHECKED_CAST")
fun <SRC: Identifiable<String>, DST: Serializable> Flow<PaginatedElement>.mapElements(mapper: (SRC) -> DST): Flow<PaginatedElement> =
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