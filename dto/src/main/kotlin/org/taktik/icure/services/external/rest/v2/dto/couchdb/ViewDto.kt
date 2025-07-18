package org.taktik.icure.services.external.rest.v2.dto.couchdb

import io.swagger.v3.oas.annotations.media.Schema

data class ViewDto(
	@get:Schema(required = true)
	val map: String,
	val reduce: String? = null,
)
