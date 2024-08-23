/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
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
import org.taktik.icure.asyncservice.TimeTableService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.entities.TimeTable
import org.taktik.icure.entities.embed.TimeTableHour
import org.taktik.icure.entities.embed.TimeTableItem
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.TimeTableDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.TimeTableV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.TimeTableBulkShareResultV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import java.util.*

@RestController("timeTableControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/timeTable")
@Tag(name = "timeTable")
class TimeTableController(
	private val timeTableService: TimeTableService,
	private val timeTableV2Mapper: TimeTableV2Mapper,
	private val bulkShareResultV2Mapper: TimeTableBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Creates a timeTable")
	@PostMapping
	fun createTimeTable(@RequestBody timeTableDto: TimeTableDto) =
		mono {
			timeTableService.createTimeTable(timeTableV2Mapper.map(timeTableDto))?.let { timeTableV2Mapper.map(it) }
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "TimeTable creation failed")
		}

	@Operation(summary = "Deletes a batch of TimeTables")
	@PostMapping("/delete/batch")
	fun deleteTimeTables(@RequestBody timeTableIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		timeTableIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
			timeTableService.deleteTimeTables(ids)
				.map(docIdentifierV2Mapper::map)
				.injectReactorContext()
		} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also { logger.error(it.message) }

	@Operation(summary = "Deletes a TimeTable")
	@DeleteMapping("/{timeTableId}")
	fun deleteTimeTable(@PathVariable timeTableId: String) = mono {
		timeTableService.deleteTimeTable(timeTableId)
			.let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Gets a timeTable")
	@GetMapping("/{timeTableId}")
	fun getTimeTable(@PathVariable timeTableId: String) =
		mono {
			if (timeTableId.equals("new", ignoreCase = true)) {
				//Create an hourItem
				val timeTableHour = TimeTableHour(
					startHour = java.lang.Long.parseLong("0800"),
					endHour = java.lang.Long.parseLong("0900")
				)

				//Create a timeTableItem
				val timeTableItem = TimeTableItem(
					rrule = null,
					calendarItemTypeId = "consult",
					hours = mutableListOf(timeTableHour)
				)
				//Create the timeTable
				val timeTable = TimeTable(
					id = UUID.randomUUID().toString(),
					startTime = java.lang.Long.parseLong("20180601000"),
					endTime = java.lang.Long.parseLong("20180801000"),
					name = "myPeriod",
					items = mutableListOf(timeTableItem)
				)

				//Return it
				timeTableV2Mapper.map(timeTable)
			} else {
				timeTableService.getTimeTable(timeTableId)?.let { timeTableV2Mapper.map(it) }
					?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "TimeTable fetching failed")
			}
		}

	@Operation(summary = "Get a list of time tables")
	@PostMapping("/byIds")
	fun getTimeTables(@RequestBody timeTableIds: ListOfIdsDto): Flux<TimeTableDto> {
		require(timeTableIds.ids.isNotEmpty()) { "You must specify at least one id" }
		return timeTableService.getTimeTables(timeTableIds.ids).map(timeTableV2Mapper::map).injectReactorContext()
	}

	@Operation(summary = "Modifies a timeTable")
	@PutMapping
	fun modifyTimeTable(@RequestBody timeTableDto: TimeTableDto) =
		mono {
			timeTableService.modifyTimeTable(timeTableV2Mapper.map(timeTableDto)).let { timeTableV2Mapper.map(it) }
		}

	@Operation(summary = "Get TimeTables by Period and AgendaId")
	@PostMapping("/byPeriodAndAgendaId")
	fun getTimeTablesByPeriodAndAgendaId(
		@Parameter(required = true) @RequestParam startDate: Long,
		@Parameter(required = true) @RequestParam endDate: Long,
		@Parameter(required = true) @RequestParam agendaId: String
	): Flux<TimeTableDto> =
		flow {
			if (agendaId.isBlank()) {
				throw ResponseStatusException(HttpStatus.BAD_REQUEST, "agendaId was empty")
			}
			emitAll(timeTableService.getTimeTablesByPeriodAndAgendaId(startDate, endDate, agendaId).map { timeTableV2Mapper.map(it) })
		}.injectReactorContext()

	@Operation(summary = "Get TimeTables by AgendaId")
	@PostMapping("/byAgendaId")
	fun getTimeTablesByAgendaId(@Parameter(required = true) @RequestParam agendaId: String): Flux<TimeTableDto> =
		flow {
			if (agendaId.isBlank()) {
				throw ResponseStatusException(HttpStatus.BAD_REQUEST, "agendaId was empty")
			}
			emitAll(timeTableService.getTimeTablesByAgendaId(agendaId).map { timeTableV2Mapper.map(it) })
		}.injectReactorContext()

	@Operation(summary = "Get the ids of the TimeTables matching the provided filter")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchTimeTablesBy(
		@RequestBody filter: AbstractFilterDto<TimeTableDto>
	) = timeTableService.matchTimeTablesBy(
		filter = filterV2Mapper.tryMap(filter).orThrow()
	).injectReactorContext()

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<TimeTableDto>> = flow {
		emitAll(timeTableService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
