package org.taktik.icure.entities.filters

import org.taktik.icure.entities.dao.KeyComponent

sealed interface AbstractCustomFilter {

	companion object {
		private const val MAX_CUSTOM_FILTER_QUERY_LIMIT = 10_000
	}

	val viewName: String
	val startKey: List<KeyComponent>?
	val startDocumentId: String?
	val limit: Int
	val entity: Class<*>

	val queryLimit: Int
		get() = limit.coerceAtMost(MAX_CUSTOM_FILTER_QUERY_LIMIT) + 1
}