package org.taktik.icure.entities.dao

data class PaginationCursor(
	val startKey: List<KeyComponent<*>>,
	val startDocumentId: String
)