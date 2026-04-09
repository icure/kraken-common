package org.taktik.icure.entities.utils

import java.io.Serializable

data class PaginatedList<T : Serializable?>(
	val pageSize: Int = 0,
	val totalSize: Int = 0,
	/** The list of results for the current page. */
	val rows: List<T> = listOf(),
	/** The key-document ID pair to use for fetching the next page of results, or null if this is the last page. */
	val nextKeyPair: PaginatedDocumentKeyIdPair<*>? = null,
) : Serializable
