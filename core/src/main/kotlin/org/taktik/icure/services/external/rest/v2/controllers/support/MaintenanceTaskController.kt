/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.MaintenanceTaskService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.MaintenanceTaskDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.MaintenanceTaskV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.MaintenanceTaskBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("maintenanceTaskControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/maintenancetask")
@Tag(name = "maintenanceTask")
class MaintenanceTaskController(
	private val filterV2Mapper: FilterV2Mapper,
	private val maintenanceTaskService: MaintenanceTaskService,
	private val maintenanceTaskV2Mapper: MaintenanceTaskV2Mapper,
	private val filterChainMapper: FilterChainV2Mapper,
	private val bulkShareResultV2Mapper: MaintenanceTaskBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val paginationConfig: SharedPaginationConfig,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val objectMapper: ObjectMapper,
) {
	@Operation(summary = "Creates a maintenanceTask")
	@PostMapping
	fun createMaintenanceTask(
		@RequestBody maintenanceTaskDto: MaintenanceTaskDto,
	) = mono {
		maintenanceTaskService
			.createMaintenanceTask(maintenanceTaskV2Mapper.map(maintenanceTaskDto))
			?.let {
				maintenanceTaskV2Mapper.map(it)
			} ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MaintenanceTask creation failed.")
	}

	@Operation(summary = "Deletes multiple MaintenanceTasks")
	@PostMapping("/delete/batch")
	fun deleteMaintenanceTasks(
		@RequestBody maintenanceTaskIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = maintenanceTaskService
		.deleteMaintenanceTasks(
			maintenanceTaskIds.ids.map { IdAndRev(it, null) },
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectReactorContext()

	@Operation(summary = "Deletes a multiple MaintenanceTasks if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteMaintenanceTasksWithRev(
		@RequestBody maintenanceTaskIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = maintenanceTaskService
		.deleteMaintenanceTasks(
			maintenanceTaskIds.ids.map(idWithRevV2Mapper::map),
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectReactorContext()

	@Operation(summary = "Deletes an MaintenanceTask")
	@DeleteMapping("/{maintenanceTaskId}")
	fun deleteMaintenanceTask(
		@PathVariable maintenanceTaskId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = mono {
		maintenanceTaskService.deleteMaintenanceTask(maintenanceTaskId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{maintenanceTaskId}")
	fun undeleteMaintenanceTask(
		@PathVariable maintenanceTaskId: String,
		@RequestParam(required = true) rev: String,
	): Mono<MaintenanceTaskDto> = mono {
		maintenanceTaskV2Mapper.map(maintenanceTaskService.undeleteMaintenanceTask(maintenanceTaskId, rev))
	}

	@DeleteMapping("/purge/{maintenanceTaskId}")
	fun purgeMaintenanceTask(
		@PathVariable maintenanceTaskId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = mono {
		maintenanceTaskService.purgeMaintenanceTask(maintenanceTaskId, rev).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Gets a maintenanceTask")
	@GetMapping("/{maintenanceTaskId}")
	fun getMaintenanceTask(
		@PathVariable maintenanceTaskId: String,
	) = mono {
		maintenanceTaskService.getMaintenanceTask(maintenanceTaskId)?.let(maintenanceTaskV2Mapper::map)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "MaintenanceTask not found")
	}

	@Operation(summary = "Retrieves multiple MaintenanceTasks by their ids")
	@PostMapping("/byIds")
	fun getMaintenanceTasks(
		@RequestBody ids: ListOfIdsDto,
	) = maintenanceTaskService
		.getMaintenanceTasks(ids.ids)
		.map(maintenanceTaskV2Mapper::map)
		.injectReactorContext()

	@Operation(summary = "Updates a maintenanceTask")
	@PutMapping
	fun modifyMaintenanceTask(
		@RequestBody maintenanceTaskDto: MaintenanceTaskDto,
	) = mono {
		maintenanceTaskService
			.modifyMaintenanceTask(maintenanceTaskV2Mapper.map(maintenanceTaskDto))
			?.let { maintenanceTaskV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "MaintenanceTask modification failed.")
	}

	@Operation(
		summary = "Filter maintenanceTasks for the current user (HcParty) ",
		description = "Returns a list of maintenanceTasks along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.",
	)
	@PostMapping("/filter")
	fun filterMaintenanceTasksBy(
		@Parameter(description = "A maintenanceTask document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<MaintenanceTaskDto>,
	) = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)

		maintenanceTaskService
			.filterMaintenanceTasks(paginationOffset, filterChainMapper.tryMap(filterChain).orThrow())
			.paginatedList(maintenanceTaskV2Mapper::map, realLimit, objectMapper = objectMapper)
	}

	@Operation(summary = "Get ids of MaintenanceTasks matching the provided filter for the current user.")
	@PostMapping("/match", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun matchMaintenanceTasksBy(
		@RequestBody filter: AbstractFilterDto<MaintenanceTaskDto>,
	) = maintenanceTaskService
		.matchMaintenanceTasksBy(
			filter = filterV2Mapper.tryMap(filter).orThrow(),
		).injectReactorContext()

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<MaintenanceTaskDto>> = flow {
		emitAll(
			maintenanceTaskService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it) },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
