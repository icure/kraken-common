/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.extra

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
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.TimeTableService
import org.taktik.icure.entities.TimeTable
import org.taktik.icure.entities.embed.TimeTableHour
import org.taktik.icure.entities.embed.TimeTableItem
import org.taktik.icure.services.external.rest.v1.dto.TimeTableDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.mapper.TimeTableMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import java.util.*

@RestController
@Profile("app")
@RequestMapping("/rest/v1/timeTable")
@Tag(name = "timeTable")
class TimeTableController(
	private val timeTableService: TimeTableService,
	private val timeTableMapper: TimeTableMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
) {
	@Operation(summary = "Creates a timeTable")
	@PostMapping
	fun createTimeTable(
		@RequestBody timeTableDto: TimeTableDto,
	) = mono {
		timeTableService.createTimeTable(timeTableMapper.map(timeTableDto))?.let { timeTableMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "TimeTable creation failed")
	}

	@Operation(summary = "Deletes a timeTable")
	@DeleteMapping("/{timeTableIds}")
	fun deleteTimeTable(
		@PathVariable timeTableIds: String,
	): Flux<DocIdentifierDto> = timeTableService
		.deleteTimeTables(timeTableIds.split(',').map { IdAndRev(it, null) })
		.map { docIdentifierMapper.map(DocIdentifier(it.id, it.rev)) }
		.injectReactorContext()

	@Operation(summary = "Gets a timeTable")
	@GetMapping("/{timeTableId}")
	fun getTimeTable(
		@PathVariable timeTableId: String,
	) = mono {
		if (timeTableId.equals("new", ignoreCase = true)) {
			// Create an hourItem
			val timeTableHour =
				TimeTableHour(
					startHour = java.lang.Long.parseLong("0800"),
					endHour = java.lang.Long.parseLong("0900"),
				)

			// Create a timeTableItem
			val timeTableItem =
				TimeTableItem(
					rrule = null,
					calendarItemTypeId = "consult",
					days = mutableListOf("monday"),
					recurrenceTypes = emptyList(),
					hours = mutableListOf(timeTableHour),
				)
			// Create the timeTable
			val timeTable =
				TimeTable(
					id = UUID.randomUUID().toString(),
					startTime = java.lang.Long.parseLong("20180601000"),
					endTime = java.lang.Long.parseLong("20180801000"),
					name = "myPeriod",
					items = mutableListOf(timeTableItem),
				)

			// Return it
			timeTableMapper.map(timeTable)
		} else {
			timeTableService.getTimeTable(timeTableId)?.let { timeTableMapper.map(it) }
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "TimeTable fetching failed")
		}
	}

	@Operation(summary = "Modifies a timeTable")
	@PutMapping
	fun modifyTimeTable(
		@RequestBody timeTableDto: TimeTableDto,
	) = mono {
		timeTableService.modifyTimeTable(timeTableMapper.map(timeTableDto)).let { timeTableMapper.map(it) }
	}

	@Operation(summary = "Get TimeTables by Period and AgendaId")
	@PostMapping("/byPeriodAndAgendaId")
	fun getTimeTablesByPeriodAndAgendaId(
		@Parameter(required = true) @RequestParam startDate: Long,
		@Parameter(required = true) @RequestParam endDate: Long,
		@Parameter(required = true) @RequestParam agendaId: String,
	): Flux<TimeTableDto> = flow {
		if (agendaId.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "agendaId was empty")
		}
		emitAll(timeTableService.getTimeTablesByPeriodAndAgendaId(startDate, endDate, agendaId).map { timeTableMapper.map(it) })
	}.injectReactorContext()

	@Operation(summary = "Get TimeTables by AgendaId")
	@PostMapping("/byAgendaId")
	fun getTimeTablesByAgendaId(
		@Parameter(required = true) @RequestParam agendaId: String,
	): Flux<TimeTableDto> = flow {
		if (agendaId.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "agendaId was empty")
		}
		emitAll(timeTableService.getTimeTablesByAgendaId(agendaId).map { timeTableMapper.map(it) })
	}.injectReactorContext()
}
