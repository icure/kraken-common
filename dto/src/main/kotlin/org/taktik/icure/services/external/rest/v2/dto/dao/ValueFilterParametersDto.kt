package org.taktik.icure.services.external.rest.v2.dto.dao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ValueFilterParametersDto(
	/**
	 * If not null, represents the key in the value object to check for filtering. If null, it means that the value is a scalar.
	 */
	@ActiveField
	val valueKey: String?,
	@ActiveField
	val range: RangeQueryParametersDto,
)
