package org.taktik.icure.utils.entities

import org.springframework.stereotype.Component
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.services.external.rest.v2.dto.FilterDefinitionDto

@Component
class FiltersDefinition(
	filterBeans: List<Filter<*, *, *>>,
) {

	val filtersByEntity: Map<String, List<FilterDefinitionDto>> = filterBeans.groupBy {
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

}