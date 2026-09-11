package org.taktik.icure.entities.dao

import com.fasterxml.jackson.databind.JsonNode

data class IdWithValue(
	val id: String,
	val value: JsonNode
)