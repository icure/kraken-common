package org.taktik.icure.services.external.rest.v2.dto.embed
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Represents a numeric range defined by a low and high bound.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.RangeDto")
data class RangeDto(
	/** The lower bound of the range. */
	@ActiveField val low: Double? = null,
	/** The upper bound of the range. */
	@ActiveField val high: Double? = null,
)
