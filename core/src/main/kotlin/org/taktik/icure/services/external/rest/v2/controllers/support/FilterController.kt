package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.services.external.rest.v2.dto.FilterDefinitionDto
import org.taktik.icure.utils.entities.FiltersDefinition

@RestController("FilterControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/filter")
@Tag(name = "filter")
class FilterController(
	private val filtersDefinition: FiltersDefinition
) {

	// AC through security config
	@GetMapping("/definitions")
	fun allFilterDefinitions(): Map<String, List<FilterDefinitionDto>> = filtersDefinition.filtersByEntity
}
