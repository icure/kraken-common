package org.taktik.icure.services.external.rest.v2.dto.couchdb

/**
 * Data transfer object representing a CouchDB design document, which contains views, lists, shows, filters,
 * and update handlers used for querying and transforming data.
 */
data class DesignDocumentDto(
	/** The identifier of the design document. */
	val id: String,
	/** The current revision of the design document. */
	val rev: String? = null,
	/** The programming language used for the design document functions. */
	val language: String? = null,
	/** A map of view names to their definitions. */
	val views: Map<String, ViewDto> = mapOf(),
	/** A map of list function names to their source code. */
	val lists: Map<String, String> = mapOf(),
	/** A map of show function names to their source code. */
	val shows: Map<String, String> = mapOf(),
	/** A map of update handler names to their source code. */
	val updateHandlers: Map<String, String>? = null,
	/** A map of filter function names to their source code. */
	val filters: Map<String, String> = mapOf(),
)
