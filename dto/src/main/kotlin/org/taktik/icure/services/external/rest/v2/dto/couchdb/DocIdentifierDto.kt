package org.taktik.icure.services.external.rest.v2.dto.couchdb

/**
 * Data transfer object that uniquely identifies a CouchDB document by its id and revision.
 */
data class DocIdentifierDto(
	/** The document identifier. */
	val id: String? = null,
	/** The document revision. */
	val rev: String? = null,
)
