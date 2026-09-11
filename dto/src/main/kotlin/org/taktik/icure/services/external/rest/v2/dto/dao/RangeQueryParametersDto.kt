package org.taktik.icure.services.external.rest.v2.dto.dao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RangeQueryParametersDto(
	/** The key component the range query starts from, inclusive. */
	@ActiveField
	val startKey: KeyComponentDto,
	/** The key component the range query ends at, inclusive. */
	@ActiveField
	val endKey: KeyComponentDto,
)
