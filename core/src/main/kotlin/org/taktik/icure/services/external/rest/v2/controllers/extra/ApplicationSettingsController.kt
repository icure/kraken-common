/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.services.external.rest.v2.dto.ApplicationSettingsDto
import org.taktik.icure.services.external.rest.v2.dto.ClassificationDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.ApplicationSettingsV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.StubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ApplicationSettingsBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ClassificationBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("applicationSettingsControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/appsettings")
@Tag(name = "applicationsettings")
class ApplicationSettingsController(
	private val applicationSettingsService: ApplicationSettingsService,
	private val applicationSettingsV2Mapper: ApplicationSettingsV2Mapper,
	private val bulkShareResultV2Mapper: ApplicationSettingsBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
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
	): Mono<ApplicationSettingsDto> = mono {
		val applicationSettings =
			applicationSettingsService.createApplicationSettings(applicationSettingsV2Mapper.map(applicationSettingsDto))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ApplicationSettings creation failed")
		applicationSettingsV2Mapper.map(applicationSettings)
	}

	@Operation(summary = "Update application settings")
	@PutMapping
	fun updateApplicationSettings(
		@RequestBody applicationSettingsDto: ApplicationSettingsDto,
	): Mono<ApplicationSettingsDto> = mono {
		val applicationSettings =
			applicationSettingsService.modifyApplicationSettings(applicationSettingsV2Mapper.map(applicationSettingsDto))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ApplicationSettings modification failed")
		applicationSettingsV2Mapper.map(applicationSettings)
	}


	@Operation(description = "Shares one or more application settings with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<ApplicationSettingsDto>> = flow {
		emitAll(
			applicationSettingsService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it) },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more application settings with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<Nothing>> = flow {
		emitAll(
			applicationSettingsService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it).minimal() },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
