/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.utils

import kotlinx.coroutines.flow.*
import org.taktik.couchdb.TotalCount
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.predicate.Predicate
import org.taktik.icure.entities.utils.PaginatedDocumentKeyIdPair
import org.taktik.icure.entities.utils.PaginatedList
import java.io.*

@Suppress("UNCHECKED_CAST")
// TODO SH MB: handle offsets
suspend fun <U : Identifiable<String>, T : Serializable> Flow<ViewQueryResultEvent>.paginatedList(mapper: (U) -> T, realLimit: Int, predicate: Predicate? = null): PaginatedList<T> {
	var viewRowCount = 0
	var lastProcessedViewRow: ViewRowWithDoc<*, *, *>? = null
	var lastProcessedViewRowNoDoc: ViewRowNoDoc<*, *>? = null

	var totalSize = 0
	var nextKeyPair: PaginatedDocumentKeyIdPair<*>? = null

	val resultRows = mutableListOf<T>()
	this.mapNotNull { viewQueryResultEvent ->
		when (viewQueryResultEvent) {
			is TotalCount -> {
				totalSize = viewQueryResultEvent.total
				null
			}
			is ViewRowWithDoc<*, *, *> -> {
				when {
					viewRowCount == realLimit -> {
						nextKeyPair = PaginatedDocumentKeyIdPair(viewQueryResultEvent.key, viewQueryResultEvent.id) // TODO SH MB: startKey was a List<String> before, now it is a String, ok?
						viewRowCount++
						lastProcessedViewRow?.doc as? U
					}
					viewRowCount < realLimit -> {
						val previous = lastProcessedViewRow
						lastProcessedViewRow = viewQueryResultEvent
						viewRowCount++
						previous?.doc as? U // if this is the first one, the Mono will be empty, so it will be ignored by flatMap
					}
					else -> { // we have more elements than expected, just ignore them
						viewRowCount++
						null
					}
				}?.takeUnless { predicate?.apply(it) == false }
			}
			is ViewRowNoDoc<*, *> -> {
				when {
					viewRowCount == realLimit -> {
						nextKeyPair = PaginatedDocumentKeyIdPair(viewQueryResultEvent.key, viewQueryResultEvent.id)
						viewRowCount++
						lastProcessedViewRowNoDoc?.id as? U
					}
					viewRowCount < realLimit -> {
						val previous = lastProcessedViewRowNoDoc
						lastProcessedViewRowNoDoc = viewQueryResultEvent
						viewRowCount++
						previous?.id as? U // if this is the first one, the Mono will be empty, so it will be ignored by flatMap
					}
					else -> { // we have more elements than expected, just ignore them
						viewRowCount++
						null
					}
				}
			}
			else -> {
				null
			}
		}
	}.map {
		mapper(it)
	}.toCollection(resultRows)

	if (resultRows.size < realLimit) {
		((lastProcessedViewRow?.doc as? U) ?: lastProcessedViewRowNoDoc?.id as U?)?.let { resultRows.add(mapper(it)) }
	}
	return PaginatedList(pageSize = realLimit, totalSize = totalSize, nextKeyPair = nextKeyPair, rows = resultRows)
}
