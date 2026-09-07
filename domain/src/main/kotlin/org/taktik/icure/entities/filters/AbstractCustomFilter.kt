package org.taktik.icure.entities.filters

import org.taktik.icure.entities.dao.KeyComponent

sealed interface AbstractCustomFilter {
	val viewName: String
	val startKey: List<KeyComponent<*>>?
	val startDocumentId: String?
	val limit: Int
	val entity: Class<*>
}