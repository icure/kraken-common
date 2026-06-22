package org.taktik.icure.services.external.rest.v2.dto.couchdb
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Data transfer object that uniquely identifies a CouchDB document by its id and revision.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto")
data class DocIdentifierDto(
	/** The document identifier. */
	@ActiveField val id: String? = null,
	/** The document revision. */
	@ActiveField val rev: String? = null,
)
