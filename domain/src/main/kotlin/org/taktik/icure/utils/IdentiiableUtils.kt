package org.taktik.icure.utils

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.StoredDocument

fun <T : Identifiable<*>> Flow<T>.distinctByIdIf(condition: Boolean): Flow<T> = if (condition) {
	distinctBy { it.id }
} else {
	this
}

fun <T : Identifiable<*>> Flow<T>.distinctById(): Flow<T> = distinctBy { it.id }

fun <T : StoredDocument> Flow<T>.subsequentDistinctById(): Flow<T> = this.subsequentDistinctBy { it.id }
