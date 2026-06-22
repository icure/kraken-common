package org.taktik.icure.services.external.rest.v2.dto.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.embed.MeasureDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object representing a pair of measures, typically used for range values or comparative measurements.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.base.MeasurePairDto")
data class MeasurePairDto(
	/** The first measure in the pair. */
	private @ActiveField val first: MeasureDto? = null,
	/** The second measure in the pair. */
	private @ActiveField val second: MeasureDto? = null,
)
