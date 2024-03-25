package org.taktik.icure.services.external.rest.v2.dto.couchdb

data class DesignDocumentDto(
	val id: String,
	val rev: String? = null,
	val revHistory: Map<String, String> = mapOf(),
	val language: String? = null,
	val views: Map<String, ViewDto> = mapOf(),
	val lists: Map<String, String> = mapOf(),
	val shows: Map<String, String> = mapOf(),
	val updateHandlers: Map<String, String>? = null,
	val filters: Map<String, String> = mapOf()
)