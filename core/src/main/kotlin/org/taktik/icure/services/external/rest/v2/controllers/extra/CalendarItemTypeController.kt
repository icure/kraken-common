/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
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
import org.taktik.icure.asyncservice.CalendarItemTypeService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.CalendarItemTypeDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.mapper.CalendarItemTypeV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("calendarItemTypeControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/calendarItemType")
@Tag(name = "calendarItemType")
class CalendarItemTypeController(
	private val calendarItemTypeService: CalendarItemTypeService,
	private val calendarItemTypeV2Mapper: CalendarItemTypeV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
	private val reactorCacheInjector: ReactorCacheInjector,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Gets all calendarItemTypes")
	@GetMapping
	fun getCalendarItemTypes(
		@Parameter(description = "A CalendarItemType document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<CalendarItemTypeDto> {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return calendarItemTypeService
			.getAllCalendarItemTypes(offset)
			.mapElements(calendarItemTypeV2Mapper::map)
			.asPaginatedFlux()
	}

	@PostMapping("/byIds")
	fun createCalendarItemTypesByIds(
		@RequestBody calendarItemTypeIds: ListOfIdsDto,
	): Flux<CalendarItemTypeDto> = calendarItemTypeService.getCalendarItemTypes(
		calendarItemTypeIds.ids
	).map(calendarItemTypeV2Mapper::map).injectReactorContext()

	@Operation(summary = "Gets calendarItemTypes for agendaId")
	@GetMapping("/byAgenda/{agendaId}")
	fun listCalendarItemTypesByAgendaId(
		@Parameter(description = "The CalendarItemType agenda ID") @PathVariable agendaId: String,
	): Flux<CalendarItemTypeDto> = calendarItemTypeService
		.listCalendarItemTypesByAgendId(agendaId)
		.map(calendarItemTypeV2Mapper::map)
		.injectReactorContext()

	@Operation(summary = "Gets all calendarItemTypes including deleted entities")
	@GetMapping("/includeDeleted")
	fun getCalendarItemTypesIncludingDeleted(
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A CalendarItemType document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<CalendarItemTypeDto> {
		val offset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return calendarItemTypeService
			.getAllEntitiesIncludeDeleted(offset)
			.mapElements(calendarItemTypeV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Creates a CalendarItemType")
	@PostMapping
	fun createCalendarItemType(
		@RequestBody calendarItemTypeDto: CalendarItemTypeDto,
	): Mono<CalendarItemTypeDto> = mono {
		calendarItemTypeV2Mapper.map(
			calendarItemTypeService.createCalendarItemType(calendarItemTypeV2Mapper.map(calendarItemTypeDto))
		)
	}

	@Operation(summary = "Creates a batch of CalendarItemTypes")
	@PostMapping("/batch")
	fun createCalendarItemTypes(
		@RequestBody calendarItemTypeDtos: List<CalendarItemTypeDto>
	): Flux<CalendarItemTypeDto> = calendarItemTypeService.createCalendarItemTypes(
		calendarItemTypeDtos.map(calendarItemTypeV2Mapper::map)
	).map(calendarItemTypeV2Mapper::map).injectReactorContext()

	@Operation(summary = "Deletes a batch of CalendarItemTypes")
	@PostMapping("/delete/batch")
	fun deleteCalendarItemTypes(
		@RequestBody calendarItemTypeIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = calendarItemTypeIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
		calendarItemTypeService
			.deleteCalendarItemTypes(ids)
			.map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
			.injectCachedReactorContext(reactorCacheInjector, 100)
	}
		?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also {
			logger.error(it.message)
		}

	@Operation(summary = "Deletes a batch of CalendarItemTypes if the current version matches the provided revs")
	@PostMapping("/delete/batch/withRev")
	fun deleteCalendarItemTypesWithRev(
		@RequestBody calendarItemTypeIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = calendarItemTypeIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
		calendarItemTypeService
			.deleteCalendarItemTypesWithRev(ids.map(idWithRevV2Mapper::map))
			.map(docIdentifierV2Mapper::map)
			.injectCachedReactorContext(reactorCacheInjector, 100)
	}
		?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also {
			logger.error(it.message)
		}

	@DeleteMapping("/{calendarItemTypeId}")
	fun deleteCalendarItemType(
		@PathVariable calendarItemTypeId: String,
		@RequestParam rev: String,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		calendarItemTypeService.deleteCalendarItemType(calendarItemTypeId, rev).let(docIdentifierV2Mapper::map)
	}

	@PostMapping("/undelete/{calendarItemTypeId}")
	fun undeleteCalendarItemType(
		@PathVariable calendarItemTypeId: String,
		@RequestParam rev: String,
	): Mono<CalendarItemTypeDto> = reactorCacheInjector.monoWithCachedContext(10) {
		calendarItemTypeService.undeleteCalendarItemType(calendarItemTypeId, rev).let(calendarItemTypeV2Mapper::map)
	}

	@PostMapping("/undelete/batch")
	fun undeleteCalendarItemTypes(
		@RequestBody calendarItemTypeIds: ListOfIdsAndRevDto,
	): Flux<CalendarItemTypeDto> = calendarItemTypeService
		.undeleteCalendarItemTypes(calendarItemTypeIds.ids.map(idWithRevV2Mapper::map))
		.map(calendarItemTypeV2Mapper::map)
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@DeleteMapping("/purge/{calendarItemTypeId}")
	fun purgeCalendarItemType(
		@PathVariable calendarItemTypeId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		calendarItemTypeService.purgeCalendarItemType(calendarItemTypeId, rev).let(docIdentifierV2Mapper::map)
	}

	@PostMapping("/purge/batch")
	fun purgeCalendarItemTypesWithRev(
		@RequestBody calendarItemTypeIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = calendarItemTypeService
		.purgeCalendarItemTypes(calendarItemTypeIds.ids.map(idWithRevV2Mapper::map))
		.map(docIdentifierV2Mapper::map)
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Gets a CalendarItemType")
	@GetMapping("/{calendarItemTypeId}")
	fun getCalendarItemType(
		@PathVariable calendarItemTypeId: String,
	): Mono<CalendarItemTypeDto> = mono {
		calendarItemTypeService.getCalendarItemType(calendarItemTypeId)?.let { calendarItemTypeV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "CalendarItemType fetching failed")
	}

	@Operation(summary = "Modifies a CalendarItemType")
	@PutMapping
	fun modifyCalendarItemType(
		@RequestBody calendarItemTypeDto: CalendarItemTypeDto,
	): Mono<CalendarItemTypeDto> = mono {
		calendarItemTypeService
			.modifyCalendarItemType(calendarItemTypeV2Mapper.map(calendarItemTypeDto))
			.let { calendarItemTypeV2Mapper.map(it) }
	}

	@Operation(summary = "Modifies a batch of CalendarItemTypes")
	@PutMapping("/batch")
	fun modifyCalendarItemTypes(
		@RequestBody calendarItemTypeDtos: List<CalendarItemTypeDto>,
	): Flux<CalendarItemTypeDto> = calendarItemTypeService.modifyCalendarItemTypes(
		calendarItemTypeDtos.map(calendarItemTypeV2Mapper::map)
	).map(calendarItemTypeV2Mapper::map).injectReactorContext()
}
