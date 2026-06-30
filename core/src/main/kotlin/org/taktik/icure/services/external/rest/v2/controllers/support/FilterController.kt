package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.services.external.rest.v2.dto.FilterDefinitionDto

@RestController("FilterControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/filter")
@Tag(name = "filter")
class FilterController(
	filterBeans: List<Filter<*, *, *>>,
) {

	private val filtersByEntity: Map<String, List<FilterDefinitionDto>> = filterBeans.groupBy {
		it.entity?.simpleName
	}.entries.mapNotNull { (entity, filters) ->
		if (entity != null) {
			entity to filters.map {
				FilterDefinitionDto(
					filter = it::class.java.simpleName,
					entity = entity,
					views = it.views
				)
			}
		} else {
			null
		}
	}.toMap()

	@GetMapping("/definitions")
	fun allFilterDefinitions(): Map<String, List<FilterDefinitionDto>> = filtersByEntity
}
