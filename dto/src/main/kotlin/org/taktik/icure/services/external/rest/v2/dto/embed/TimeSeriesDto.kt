package org.taktik.icure.services.external.rest.v2.dto.embed
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

import org.taktik.icure.ExposedToCustomEntities

/**
 * Represents a high-frequency time series with named fields, sample data, and statistical summaries.
 */
@ExposedToCustomEntities
data class TimeSeriesDto(
	/** The names of the fields (columns) in the time series. */
	@ActiveField val fields: List<String> = emptyList(),
	/** The sample data, where each inner list represents one sample across all fields. */
	@ActiveField val samples: List<List<Double>> = emptyList(),
	/** The minimum values for each field. */
	@ActiveField val min: List<Double> = emptyList(),
	/** The maximum values for each field. */
	@ActiveField val max: List<Double> = emptyList(),
	/** The mean values for each field. */
	@ActiveField val mean: List<Double> = emptyList(),
	/** The median values for each field. */
	@ActiveField val median: List<Double> = emptyList(),
	/** The variance values for each field. */
	@ActiveField val variance: List<Double> = emptyList(),
)
