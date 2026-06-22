package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Metadata describing a single filter exposed by the API.
 *
 * @property filter the name of the concrete filter DTO type.
 * @property entity the simple name of the entity owning the configuration view(s) this filter relies on (null if none).
 * @property views the design-doc configuration view(s) this filter relies on, all defined on [entity] (empty if none).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.FilterDefinitionDto")
data class FilterDefinitionDto(
	@ActiveField val filter: String,
	@ActiveField val entity: String? = null,
	@ActiveField val views: List<String> = emptyList(),
)
