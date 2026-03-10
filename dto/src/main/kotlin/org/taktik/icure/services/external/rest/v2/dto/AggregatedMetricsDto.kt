package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * DTO containing aggregated metrics information for a group or environment.
 */
data class AggregatedMetricsDto(
	/** The number of currently active entities. */
	val activeEntitiesCount: Long? = null,
)
