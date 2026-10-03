package org.taktik.icure.services.external.rest.v2.dto.dao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * A view row identifier paired with its associated value.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class IdWithValueDto(
	/** The id of the row. */
	@ActiveField
	val id: String,
	/** The value associated to the row. */
	@ActiveField
	val value: JsonNode,
)
