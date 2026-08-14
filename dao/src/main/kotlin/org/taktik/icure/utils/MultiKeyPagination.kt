package org.taktik.icure.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRow
import org.taktik.couchdb.entity.ViewQuery

/**
 * Runs a paginated multi-key ("by keys") view query: queries [keys] in the order they are given, returning at most
 * [limit] rows in total, resuming from [startDocumentId] where a previous page stopped.
 *
 * # Why this can't be a single query
 *
 * CouchDB applies `startkey_docid` to *every* key of a multi-key query, not only to the first one, so passing it
 * together with all the keys would silently drop the rows of the other keys that happen to have a document id lower
 * than it. Only the first key may have been partially returned by the previous page, so it is queried on its own —
 * as an explicit single-key range rather than through `keys`, since a start document id is only documented to be
 * honoured together with a start key — and the remaining keys are queried without a start document id, with the
 * limit reduced by what the first key already returned. A page therefore costs two queries when it resumes and one
 * otherwise.
 *
 * The caller is expected to ask for one more row than the page it wants to return, and to turn that extra row into
 * the cursor of the next page; the keys still to visit are the ones from that row's key onwards, which is why the
 * order of [keys] is part of the contract. See `Flow<ViewQueryResultEvent>.toMultiKeyPaginatedFlow` in the
 * pagination package for the other half of that mechanism.
 *
 * @param keys the view keys to query, in the order they should be visited, without duplicates. [startDocumentId]
 * applies to the first of them.
 * @param keysDescription how to name [keys] in the error messages of this method, as a plural noun phrase (e.g.
 * "recipients"), so that a caller-facing error says what the caller actually passed.
 * @param startDocumentId the id of the first document to return for the first of [keys], or null to start from the
 * beginning.
 * @param limit the maximum total number of rows to return, over all of [keys].
 * @param viewQuery builds the query to run, without keys, limit or start document id: it is called once per
 * underlying query, so it must return an equivalent query every time.
 * @param executeQuery runs a query built by [viewQuery]. It is up to the caller, since the key, value and document
 * types are known only there.
 * @throws IllegalArgumentException if [keys] is empty or has duplicates, or if [limit] is not positive.
 */
fun multiKeyPaginatedViewQuery(
	keys: List<Any?>,
	keysDescription: String,
	startDocumentId: String?,
	limit: Int,
	viewQuery: suspend () -> ViewQuery,
	executeQuery: (query: ViewQuery) -> Flow<ViewQueryResultEvent>,
): Flow<ViewQueryResultEvent> = flow {
	require(keys.isNotEmpty()) {
		"At least one of the $keysDescription should be provided."
	}
	require(keys.distinct().size == keys.size) {
		"The $keysDescription should not contain duplicates."
	}
	require(limit > 0) {
		"The limit should be positive."
	}
	if (startDocumentId == null) {
		emitAll(executeQuery(viewQuery().keys(keys).limit(limit)))
	} else {
		val returnedForFirstKey = executeQuery(
			viewQuery()
				.startKey(keys.first())
				.endKey(keys.first())
				.startDocId(startDocumentId)
				.limit(limit),
		).onEach { emit(it) }.count { it is ViewRow<*, *, *> }
		if (keys.size > 1 && returnedForFirstKey < limit) {
			emitAll(
				executeQuery(
					viewQuery()
						.keys(keys.drop(1))
						.limit(limit - returnedForFirstKey),
				),
			)
		}
	}
}
