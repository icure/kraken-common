package org.taktik.icure.entities.utils

import java.io.Serializable

data class PaginatedList<T : Serializable?>(
	val pageSize: Int = 0,
	val totalSize: Int = 0,
	val rows: List<T> = listOf(),
	val nextKeyPair: PaginatedDocumentKeyIdPair<*>? = null,
) : Serializable
