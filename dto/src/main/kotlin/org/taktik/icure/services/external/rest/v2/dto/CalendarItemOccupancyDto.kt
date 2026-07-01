package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * One point of a concurrent-occupancy step function for a period of calendar items.
 *
 * The occupancy of a period is emitted as a sequence of these points, ordered by [timestamp]: each point
 * indicates that, starting from [timestamp], the number of overlapping (busy) calendar items becomes [occupancy].
 */
data class CalendarItemOccupancyDto(
	/** A fuzzy date-time at which the occupancy changes. */
	val timestamp: Long,
	/** The number of calendar items that are concurrently busy starting from [timestamp]. */
	val occupancy: Long,
)
