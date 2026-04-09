package org.taktik.icure.entities.embed

data class TimeSeries(
	/** The names of the fields (columns) in the time series. */
	val fields: List<String> = emptyList(),
	/** The sample data, where each inner list represents one sample across all fields. */
	val samples: List<List<Double>> = emptyList(),
	/** The minimum values for each field. */
	val min: List<Double> = emptyList(),
	/** The maximum values for each field. */
	val max: List<Double> = emptyList(),
	/** The mean values for each field. */
	val mean: List<Double> = emptyList(),
	/** The median values for each field. */
	val median: List<Double> = emptyList(),
	/** The variance values for each field. */
	val variance: List<Double> = emptyList(),
)
