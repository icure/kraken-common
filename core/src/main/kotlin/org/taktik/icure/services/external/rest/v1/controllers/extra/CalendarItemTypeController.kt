/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncservice.CalendarItemTypeService
import org.taktik.icure.services.external.rest.v1.dto.CalendarItemTypeDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.mapper.CalendarItemTypeMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/calendarItemType")
@Tag(name = "calendarItemType")
class CalendarItemTypeController(
    private val calendarItemTypeService: CalendarItemTypeService,
    private val calendarItemTypeMapper: CalendarItemTypeMapper,
    private val docIdentifierMapper: DocIdentifierMapper,
) {

    @Operation(summary = "Gets all calendarItemTypes")
    @GetMapping
    fun getCalendarItemTypes() = calendarItemTypeService
        .getAllCalendarItemTypes()
        .map(calendarItemTypeMapper::map)
        .injectReactorContext()

    @Operation(summary = "Gets calendarItemTypes for agendaId")
    @GetMapping("/byAgenda/{agendaId}")
    fun listCalendarItemTypesByAgendaId(
        @Parameter(description = "The CalendarItemType agenda ID") @PathVariable agendaId: String
    ): Flux<CalendarItemTypeDto> {
        return calendarItemTypeService
            .listCalendarItemTypesByAgendId(agendaId)
            .map(calendarItemTypeMapper::map)
            .injectReactorContext()
    }

    @Operation(summary = "Gets all calendarItemTypes including deleted entities")
    @GetMapping("/includeDeleted")
    fun getCalendarItemTypesIncludingDeleted() = calendarItemTypeService
        .getAllEntitiesIncludeDeleted()
        .map(calendarItemTypeMapper::map)
        .injectReactorContext()

    @Operation(summary = "Creates a calendarItemType")
    @PostMapping
    fun createCalendarItemType(@RequestBody calendarItemTypeDto: CalendarItemTypeDto) = mono {
        calendarItemTypeService.createCalendarItemType(calendarItemTypeMapper.map(calendarItemTypeDto))
            ?.let { calendarItemTypeMapper.map(it) }
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CalendarItemType creation failed")
    }

    @Operation(summary = "Deletes a calendarItemType")
    @DeleteMapping("/{calendarItemTypeIds}")
    fun deleteCalendarItemType(@PathVariable calendarItemTypeIds: String): Flux<DocIdentifierDto> =
        calendarItemTypeService.deleteCalendarItemTypes(calendarItemTypeIds.split(','))
            .map(docIdentifierMapper::map)
            .injectReactorContext()

    @Operation(summary = "Gets a calendarItemType")
    @GetMapping("/{calendarItemTypeId}")
    fun getCalendarItemType(@PathVariable calendarItemTypeId: String) = mono {
        calendarItemTypeService.getCalendarItemType(calendarItemTypeId)?.let { calendarItemTypeMapper.map(it) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "CalendarItemType fetching failed")
    }

    @Operation(summary = "Modifies a calendarItemType")
    @PutMapping
    fun modifyCalendarItemType(@RequestBody calendarItemTypeDto: CalendarItemTypeDto) = mono {
        calendarItemTypeService.modifyCalendarItemType(calendarItemTypeMapper.map(calendarItemTypeDto))
            ?.let { calendarItemTypeMapper.map(it) }
            ?: throw DocumentNotFoundException("CalendarItemType modification failed")
    }
}
