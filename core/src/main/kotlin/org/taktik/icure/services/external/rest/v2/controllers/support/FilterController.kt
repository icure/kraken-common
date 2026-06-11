package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController("FilterControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/filter")
@Tag(name = "filter")
class FilterController(
	private val filterDefinitionsFactory: FilterDefinitionsFactory,
) {
	@GetMapping("/definitions")
	fun allFilterDefinitions() = filterDefinitionsFactory.definitions
}
