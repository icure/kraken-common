package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TimeSeriesDto(
	val fields: List<String> = emptyList(),
	val samples: List<List<Double>> = emptyList(),
	val min: List<Double> = emptyList(),
	val max: List<Double> = emptyList(),
	val mean: List<Double> = emptyList(),
	val median: List<Double> = emptyList(),
	val variance: List<Double> = emptyList(),
): Serializable
