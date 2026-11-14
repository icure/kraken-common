package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ViewResourcesDto(
	@ActiveField
	val filters: Map<String, List<FilterDefinitionDto>>,
	/**
	 * Rest endpoint to view(s).
	 */
	@ActiveField
	val legacy: Map<String, List<ViewsForEntityDto>>,
	@ActiveField
	val additionalFeatures: Map<String, List<ViewsForEntityDto>>
) {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	data class ViewsForEntityDto(
		@ActiveField
		val entity: String,
		@ActiveField
		val views: List<String>
	)

}
