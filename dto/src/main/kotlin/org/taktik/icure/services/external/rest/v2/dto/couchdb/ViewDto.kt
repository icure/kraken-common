package org.taktik.icure.services.external.rest.v2.dto.couchdb

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Data transfer object representing a CouchDB view definition within a design document.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.couchdb.ViewDto")
data class ViewDto(
	/** The map function source code for this view. */
	@param:Schema(required = true)
	@ActiveField val map: String,
	/** The optional reduce function source code for this view. */
	@ActiveField val reduce: String? = null,
)
