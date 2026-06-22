package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * DTO containing aggregated metrics information for a group or environment.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.AggregatedMetricsDto")
data class AggregatedMetricsDto(
	/** The number of currently active entities. */
	@ActiveField val activeEntitiesCount: Long? = null,
)
