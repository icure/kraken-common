package org.taktik.icure.services.external.rest.v2.dto.couchdb

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Data transfer object representing a CouchDB view definition within a design document.
 */
data class ViewDto(
	/** The map function source code for this view. */
	@param:Schema(required = true)
	val map: String,
	/** The optional reduce function source code for this view. */
	val reduce: String? = null,
)
