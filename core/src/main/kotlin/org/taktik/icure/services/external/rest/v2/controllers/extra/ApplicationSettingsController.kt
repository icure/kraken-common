/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.asyncservice.ApplicationSettingsService
import org.taktik.icure.services.external.rest.v2.dto.ApplicationSettingsDto
import org.taktik.icure.services.external.rest.v2.mapper.ApplicationSettingsV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController("applicationSettingsControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/appsettings")
@Tag(name = "applicationsettings")
class ApplicationSettingsController(
	private val applicationSettingsService: ApplicationSettingsService,
	private val applicationSettingsV2Mapper: ApplicationSettingsV2Mapper,
) {
	@Operation(summary = "Gets all application settings")
	@GetMapping
	fun getApplicationSettings(): Flux<ApplicationSettingsDto> = applicationSettingsService
		.getAllApplicationSettings()
		.map(applicationSettingsV2Mapper::map)
		.injectReactorContext()

	@Operation(summary = "Create new application settings")
	@PostMapping
	fun createApplicationSettings(
		@RequestBody applicationSettingsDto: ApplicationSettingsDto,
	) = mono {
		val applicationSettings =
			applicationSettingsService.createApplicationSettings(applicationSettingsV2Mapper.map(applicationSettingsDto))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ApplicationSettings creation failed")
		applicationSettingsV2Mapper.map(applicationSettings)
	}

	@Operation(summary = "Update application settings")
	@PutMapping
	fun updateApplicationSettings(
		@RequestBody applicationSettingsDto: ApplicationSettingsDto,
	) = mono {
		val applicationSettings =
			applicationSettingsService.modifyApplicationSettings(applicationSettingsV2Mapper.map(applicationSettingsDto))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ApplicationSettings modification failed")
		applicationSettingsV2Mapper.map(applicationSettings)
	}
}
