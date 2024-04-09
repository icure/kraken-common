package org.taktik.icure.services.external.rest.v1.dto.couchdb

data class ViewDto(
	val map: String,
	val reduce: String? = null
)