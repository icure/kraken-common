package org.taktik.icure.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.taktik.couchdb.id.Identifiable

fun <T : Identifiable<*>> mergeUniqueValuesForSearchKeys(
	searchKeys: Set<String>,
	listFunction: (String) -> Flow<T>,
): Flow<T> = if (searchKeys.size > 1) {
	flow { searchKeys.forEach { emitAll(listFunction(it)) } }.distinctById()
} else {
	listFunction(searchKeys.first())
}

fun mergeUniqueIdsForSearchKeys(
	searchKeys: Set<String>,
	listFunction: (String) -> Flow<String>,
): Flow<String> = if (searchKeys.size > 1) {
	flow { searchKeys.forEach { emitAll(listFunction(it)) } }.distinct()
} else {
	listFunction(searchKeys.first())
}
