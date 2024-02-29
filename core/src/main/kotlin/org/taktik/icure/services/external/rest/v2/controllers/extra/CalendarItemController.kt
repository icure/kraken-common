/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
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
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncservice.CalendarItemService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.CalendarItemDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.CalendarItemV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.CalendarItemBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.StartKeyJsonString
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.paginatedList
import reactor.core.publisher.Flux

@RestController("calendarItemControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/calendarItem")
@Tag(name = "calendarItem")
class CalendarItemController(
	private val calendarItemService: CalendarItemService,
	private val calendarItemV2Mapper: CalendarItemV2Mapper,
	private val bulkShareResultV2Mapper: CalendarItemBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val objectMapper: ObjectMapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val paginationConfig: SharedPaginationConfig
) {

	@Operation(summary = "Gets all calendarItems")
	@GetMapping
	fun getCalendarItems(
		@Parameter(description = "A CalendarItem document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return calendarItemService
			.getAllCalendarItems(offset)
			.mapElements(calendarItemV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Creates a calendarItem")
	@PostMapping
	fun createCalendarItem(@RequestBody calendarItemDto: CalendarItemDto) = mono {
		val calendarItem = calendarItemService.createCalendarItem(calendarItemV2Mapper.map(calendarItemDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CalendarItem creation failed")

		calendarItemV2Mapper.map(calendarItem)
	}

	@Operation(summary = "Deletes a batch of calendarItems")
	@PostMapping("/delete/batch")
	fun deleteCalendarItems(@RequestBody calendarItemIds: ListOfIdsDto): Flux<DocIdentifier> =
		calendarItemService.deleteCalendarItems(calendarItemIds.ids).injectReactorContext()

	@Operation(summary = "Deletes a calendarItem")
	@DeleteMapping("/{calendarItemId}")
	fun deleteCalendarItem(@PathVariable calendarItemId: String) = mono {
		calendarItemService.deleteCalendarItem(calendarItemId)
	}

	@Deprecated(message = "Use deleteItemCalendars instead")
	@Operation(summary = "Deletes an calendarItem")
	@PostMapping("/{calendarItemIds}")
	fun deleteCalendarItemsWithPost(@PathVariable calendarItemIds: String): Flux<DocIdentifier> =
		calendarItemService.deleteCalendarItems(calendarItemIds.split(',')).injectReactorContext()

	@Operation(summary = "Gets an calendarItem")
	@GetMapping("/{calendarItemId}")
	fun getCalendarItem(@PathVariable calendarItemId: String) = mono {
		val calendarItem = calendarItemService.getCalendarItem(calendarItemId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "CalendarItem fetching failed")

		calendarItemV2Mapper.map(calendarItem)
	}

	@Operation(summary = "Modifies an calendarItem")
	@PutMapping
	fun modifyCalendarItem(@RequestBody calendarItemDto: CalendarItemDto) = mono {
		val calendarItem = calendarItemService.modifyCalendarItem(calendarItemV2Mapper.map(calendarItemDto))
			?: throw DocumentNotFoundException("CalendarItem modification failed")

		calendarItemV2Mapper.map(calendarItem)
	}

	@Operation(summary = "Get CalendarItems by Period and HcPartyId")
	@PostMapping("/byPeriodAndHcPartyId")
	fun getCalendarItemsByPeriodAndHcPartyId(
		@RequestParam startDate: Long,
		@RequestParam endDate: Long,
		@RequestParam hcPartyId: String
	): Flux<CalendarItemDto> {
		if (hcPartyId.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "hcPartyId was empty")
		}
		val calendars = calendarItemService.getCalendarItemByPeriodAndHcPartyId(startDate, endDate, hcPartyId)
		return calendars.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Get CalendarItems by Period and AgendaId")
	@PostMapping("/byPeriodAndAgendaId")
	fun getCalendarsByPeriodAndAgendaId(
		@RequestParam startDate: Long,
		@RequestParam endDate: Long,
		@RequestParam agendaId: String
	): Flux<CalendarItemDto> {
		if (agendaId.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "agendaId was empty")
		}
		val calendars = calendarItemService.getCalendarItemByPeriodAndAgendaId(startDate, endDate, agendaId)
		return calendars.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Get calendarItems by ids")
	@PostMapping("/byIds")
	fun getCalendarItemsWithIds(@RequestBody calendarItemIds: ListOfIdsDto?): Flux<CalendarItemDto> {
		if (calendarItemIds == null) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "calendarItemIds was empty")
		}
		val calendars = calendarItemService.getCalendarItems(calendarItemIds.ids)
		return calendars.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Find CalendarItems by hcparty and patient", description = "")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun listCalendarItemsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
	): Flux<CalendarItemDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val elementList = calendarItemService.listCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId, ArrayList(secretPatientKeys))

		return elementList.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Find CalendarItems by hcparty and patient")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun listCalendarItemsByHCPartyPatientForeignKeys(@RequestParam hcPartyId: String, @RequestBody secretPatientKeys: List<String>): Flux<CalendarItemDto> {
		val elementList = calendarItemService.listCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId, ArrayList(secretPatientKeys))

		return elementList.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Find CalendarItems by hcparty and patient", description = "")
	@GetMapping("/byHcPartySecretForeignKeys/page/{limit}")
	fun findCalendarItemsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
		@Parameter(description = "The start key for pagination: a JSON representation of an array containing all the necessary " + "components to form the Complex Key's startKey") @RequestParam(required = false) startKey: StartKeyJsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @PathVariable limit: Int,
	) = mono {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val startKeyElements = startKey?.let { objectMapper.readValue<List<Any>>(startKey) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit + 1)
		val elementList = calendarItemService.findCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys, paginationOffset)

		elementList.paginatedList(calendarItemV2Mapper::map, limit)
	}

	@Operation(summary = "Find CalendarItems by hcparty and patient")
	@PostMapping("/byHcPartySecretForeignKeys/page/{limit}")
	fun findCalendarItemsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
		@Parameter(description = "The start key for pagination: a JSON representation of an array containing all the necessary " + "components to form the Complex Key's startKey") @RequestParam(required = false) startKey: StartKeyJsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @PathVariable limit: Int,
	) = mono {
		val startKeyElements = startKey?.let { objectMapper.readValue<List<Any>>(startKey) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit + 1)
		val elementList = calendarItemService.findCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys, paginationOffset)

		elementList.paginatedList(calendarItemV2Mapper::map, limit)
	}

	@Operation(summary = "Find CalendarItems by recurrenceId", description = "")
	@GetMapping("/byRecurrenceId")
	fun findCalendarItemsByRecurrenceId(@RequestParam recurrenceId: String): Flux<CalendarItemDto> {
		val elementList = calendarItemService.getCalendarItemsByRecurrenceId(recurrenceId)
		return elementList.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<CalendarItemDto>> = flow {
		emitAll(calendarItemService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more calendar items with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<CalendarItemDto>> = flow {
		emitAll(calendarItemService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it).copy(updatedEntity = null) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
