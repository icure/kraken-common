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
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
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
import org.taktik.icure.asyncservice.CalendarItemService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.utils.PaginatedList
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.CalendarItemDto
import org.taktik.icure.services.external.rest.v2.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.CalendarItemV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.StubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.CalendarItemBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import org.taktik.icure.utils.paginatedList
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("calendarItemControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/calendarItem")
@Tag(name = "calendarItem")
class CalendarItemController(
	private val calendarItemService: CalendarItemService,
	private val calendarItemV2Mapper: CalendarItemV2Mapper,
	private val bulkShareResultV2Mapper: CalendarItemBulkShareResultV2Mapper,
	private val stubV2Mapper: StubV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val objectMapper: ObjectMapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	@Operation(summary = "Gets all calendarItems")
	@GetMapping
	fun getCalendarItems(
		@Parameter(description = "A CalendarItem document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<CalendarItemDto> {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return calendarItemService
			.getAllCalendarItems(offset)
			.mapElements(calendarItemV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Creates a CalendarItem")
	@PostMapping
	fun createCalendarItem(
		@RequestBody calendarItemDto: CalendarItemDto,
	): Mono<CalendarItemDto> = mono {
		calendarItemV2Mapper.map(calendarItemService.createCalendarItem(calendarItemV2Mapper.map(calendarItemDto)))
	}

	@Operation(summary = "Creates a batch of CalendarItems")
	@PostMapping("/batch")
	fun createCalendarItems(
		@RequestBody calendarItemDtos: List<CalendarItemDto>,
	): Flux<CalendarItemDto> = calendarItemService.createCalendarItems(
		calendarItemDtos.map(calendarItemV2Mapper::map)
	).map(calendarItemV2Mapper::map).injectReactorContext()

	@Operation(summary = "Deletes multiple CalendarItems")
	@PostMapping("/delete/batch")
	fun deleteCalendarItems(
		@RequestBody calendarItemIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = calendarItemService
		.deleteCalendarItems(
			calendarItemIds.ids.map { IdAndRev(it, null) },
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Delete multiple CalendarItems if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteCalendarItemsWithRev(
		@RequestBody calendarItemIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = calendarItemService
		.deleteCalendarItems(
			calendarItemIds.ids.map(idWithRevV2Mapper::map),
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Delete a CalendarItem")
	@DeleteMapping("/{calendarItemId}")
	fun deleteCalendarItem(
		@PathVariable calendarItemId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		calendarItemService.deleteCalendarItem(calendarItemId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{calendarItemId}")
	fun undeleteCalendarItem(
		@PathVariable calendarItemId: String,
		@RequestParam(required = true) rev: String,
	): Mono<CalendarItemDto> = reactorCacheInjector.monoWithCachedContext(10) {
		calendarItemV2Mapper.map(calendarItemService.undeleteCalendarItem(calendarItemId, rev))
	}

	@Operation(summary = "Undelete multiple CalendarItems if they match the provided revs")
	@PostMapping("/undelete/batch")
	fun undeleteCalendarItems(
		@RequestBody calendarItemIds: ListOfIdsAndRevDto,
	): Flux<CalendarItemDto> = calendarItemService
		.undeleteCalendarItems(
			calendarItemIds.ids.map(idWithRevV2Mapper::map),
		).map { calendarItemV2Mapper.map(it) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@DeleteMapping("/purge/{calendarItemId}")
	fun purgeCalendarItem(
		@PathVariable calendarItemId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		calendarItemService.purgeCalendarItem(calendarItemId, rev).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Purge multiple CalendarItems if they match the provided revs")
	@PostMapping("/purge/batch")
	fun purgeCalendarItems(
		@RequestBody calendarItemIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = calendarItemService
		.purgeCalendarItems(
			calendarItemIds.ids.map(idWithRevV2Mapper::map),
		).map { docIdentifierV2Mapper.map(it) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Gets a calendarItem")
	@GetMapping("/{calendarItemId}")
	fun getCalendarItem(
		@PathVariable calendarItemId: String,
	): Mono<CalendarItemDto> = mono {
		val calendarItem =
			calendarItemService.getCalendarItem(calendarItemId)
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "CalendarItem fetching failed")

		calendarItemV2Mapper.map(calendarItem)
	}

	@Operation(summary = "Modifies a CalendarItem")
	@PutMapping
	fun modifyCalendarItem(
		@RequestBody calendarItemDto: CalendarItemDto,
	): Mono<CalendarItemDto> = mono {
		val calendarItem = calendarItemService.modifyCalendarItem(calendarItemV2Mapper.map(calendarItemDto))
		calendarItemV2Mapper.map(calendarItem)
	}

	@Operation(summary = "Modifies a batch of CalendarItems")
	@PutMapping("/batch")
	fun modifyCalendarItems(
		@RequestBody calendarItemDtos: List<CalendarItemDto>,
	): Flux<CalendarItemDto> = calendarItemService.modifyCalendarItems(
		calendarItemDtos.map(calendarItemV2Mapper::map)
	).map(calendarItemV2Mapper::map).injectReactorContext()

	@Operation(summary = "Get CalendarItems by Period and HcPartyId")
	@PostMapping("/byPeriodAndHcPartyId")
	fun getCalendarItemsByPeriodAndHcPartyId(
		@RequestParam startDate: Long,
		@RequestParam endDate: Long,
		@RequestParam hcPartyId: String,
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
		@RequestParam agendaId: String,
	): Flux<CalendarItemDto> {
		if (agendaId.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "agendaId was empty")
		}
		val calendars = calendarItemService.getCalendarItemByPeriodAndAgendaId(startDate, endDate, agendaId)
		return calendars.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Get calendarItems by ids")
	@PostMapping("/byIds")
	fun getCalendarItemsWithIds(
		@RequestBody calendarItemIds: ListOfIdsDto,
	): Flux<CalendarItemDto> {
		require(calendarItemIds.ids.isNotEmpty()) { "You must specify at least one id" }
		val calendars = calendarItemService.getCalendarItems(calendarItemIds.ids)
		return calendars.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Find CalendarItems by hcparty and patient", description = "")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun listCalendarItemsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
	): Flux<CalendarItemDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val elementList = calendarItemService.listCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId, ArrayList(secretPatientKeys))

		return elementList.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Find CalendarItems by hcparty and patient")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun listCalendarItemsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<CalendarItemDto> {
		val elementList = calendarItemService.listCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId, ArrayList(secretPatientKeys))

		return elementList.map { calendarItemV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Find CalendarItems by hcparty and patient", description = "")
	@GetMapping("/byHcPartySecretForeignKeys/page/{limit}")
	fun findCalendarItemsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
		@Parameter(
			description =
			"The start key for pagination: a JSON representation of an array containing all the necessary " +
				"components to form the Complex Key's startKey",
		) @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @PathVariable limit: Int,
	): Mono<PaginatedList<CalendarItemDto>> = mono {
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
		@Parameter(
			description =
			"The start key for pagination: a JSON representation of an array containing all the necessary " +
				"components to form the Complex Key's startKey",
		) @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @PathVariable limit: Int,
	): Mono<PaginatedList<CalendarItemDto>> = mono {
		val startKeyElements = startKey?.let { objectMapper.readValue<List<Any>>(startKey) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit + 1)
		val elementList = calendarItemService.findCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys, paginationOffset)

		elementList.paginatedList(calendarItemV2Mapper::map, limit)
	}

	@Operation(summary = "Find CalendarItems ids by data owner id, patient secret keys and start time")
	@PostMapping("/byDataOwnerPatientStartTime", produces = [APPLICATION_JSON_VALUE])
	fun findCalendarItemIdsByDataOwnerPatientStartTime(
		@RequestParam dataOwnerId: String,
		@RequestParam(required = false) startDate: Long?,
		@RequestParam(required = false) endDate: Long?,
		@RequestParam(required = false) descending: Boolean?,
		@RequestBody secretPatientKeys: ListOfIdsDto,
	): Flux<String> {
		require(secretPatientKeys.ids.isNotEmpty()) {
			"You need to provide at least one secret patient key"
		}
		return calendarItemService
			.findCalendarItemIdsByDataOwnerPatientStartTime(
				dataOwnerId = dataOwnerId,
				secretForeignKeys = secretPatientKeys.ids.toSet(),
				startDate = startDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				endDate = endDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				descending = descending ?: false,
			).injectReactorContext()
	}

	@Operation(summary = "List calendar items stubs found by healthcare party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun findCalendarItemsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> = calendarItemService
		.listCalendarItemsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)
		.map { calendarItem -> stubV2Mapper.mapToStub(calendarItem) }
		.injectReactorContext()

	@Operation(summary = "List calendar items stubs by ids")
	@PostMapping("/delegations")
	fun findCalendarItemsDelegationsStubsByIds(
		@RequestBody calendarItemIds: ListOfIdsDto,
	): Flux<IcureStubDto> = calendarItemService
		.getCalendarItems(calendarItemIds.ids)
		.map { calendarItem -> stubV2Mapper.mapToStub(calendarItem) }
		.injectReactorContext()

	@Operation(summary = "Find CalendarItems by recurrenceId with pagination")
	@GetMapping("/byRecurrenceId")
	fun findCalendarItemsByRecurrenceId(
		@RequestParam recurrenceId: String,
		@Parameter(
			description =
			"The start key for pagination: a JSON representation of an array containing all the necessary " +
				"components to form the Complex Key's startKey",
		) @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<CalendarItemDto> = calendarItemService
		.getCalendarItemsByRecurrenceId(recurrenceId, PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit))
		.mapElements(calendarItemV2Mapper::map)
		.asPaginatedFlux()

	@Operation(summary = "Get the ids of the CalendarItems matching the provided filter.")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchCalendarItemsBy(
		@RequestBody filter: AbstractFilterDto<CalendarItemDto>,
	): Flux<String> = calendarItemService
		.matchCalendarItemsBy(
			filter = filterV2Mapper.tryMap(filter).orThrow(),
		).injectReactorContext()

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<CalendarItemDto>> = flow {
		emitAll(
			calendarItemService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it) },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more calendar items with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<Nothing>> = flow {
		emitAll(
			calendarItemService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it).minimal() },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
