/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.firstOrNull
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

import org.taktik.icure.asyncservice.AgendaService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.AgendaDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.TimeTableDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.mapper.AgendaV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux

@RestController("agendaControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/agenda")
@Tag(name = "agenda")
class AgendaController(
	private val agendaService: AgendaService,
	private val agendaV2Mapper: AgendaV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val paginationConfig: SharedPaginationConfig
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Gets all agendas")
	@GetMapping
	fun getAgendas(
		@Parameter(description = "An agenda document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux<AgendaDto> {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return agendaService
			.getAllAgendas(offset)
			.mapElements(agendaV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Creates a agenda")
	@PostMapping
	fun createAgenda(@RequestBody agendaDto: AgendaDto) = mono {
		val agenda = agendaService.createAgenda(agendaV2Mapper.map(agendaDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agenda creation failed")

		agendaV2Mapper.map(agenda)
	}

	@Operation(summary = "Deletes a batch of agendas")
	@PostMapping("/delete/batch")
	fun deleteAgendas(@RequestBody agendaIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		agendaIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
			agendaService.deleteAgendas(ids.toSet()).map(docIdentifierV2Mapper::map).injectReactorContext()
		} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also { logger.error(it.message) }

	@Operation(summary = "Deletes a single agenda")
	@DeleteMapping("/{agendaId}")
	fun deleteAgenda(@PathVariable agendaId: String) = mono {
		agendaService.deleteAgenda(agendaId).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Gets an agenda")
	@GetMapping("/{agendaId}")
	fun getAgenda(@PathVariable agendaId: String) = mono {
		val agenda = agendaService.getAgenda(agendaId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Agenda fetching failed")
		agendaV2Mapper.map(agenda)
	}

	@Operation(summary = "Gets all agendas for user")
	@GetMapping("/byUser")
	fun getAgendasForUser(@RequestParam userId: String) = mono {
		agendaService.getAgendasByUser(userId).firstOrNull()?.let { agendaV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Agendas fetching failed")
	}

	@Operation(summary = "Gets readable agendas for user")
	@GetMapping("/readableForUser")
	fun getReadableAgendasForUser(@RequestParam userId: String): Flux<AgendaDto> {
		val agendas = agendaService.getReadableAgendaForUser(userId)
		return agendas.map { agendaV2Mapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Modifies an agenda")
	@PutMapping
	fun modifyAgenda(@RequestBody agendaDto: AgendaDto) = mono {
		val agenda = agendaService.modifyAgenda(agendaV2Mapper.map(agendaDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agenda modification failed")
		agendaV2Mapper.map(agenda)
	}

	@Operation(summary = "Get the ids of the Agendas matching the provided filter")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchAgendasBy(
		@RequestBody filter: AbstractFilterDto<AgendaDto>
	) = agendaService.matchAgendasBy(
		filter = filterV2Mapper.tryMap(filter).orThrow(),
	).injectReactorContext()

	@Operation(summary = "Get agendas by ids")
	@PostMapping("/byIds")
	fun getAgendasByIds(@RequestBody agendaIds: ListOfIdsDto): Flux<AgendaDto> {
		require(agendaIds.ids.isNotEmpty()) { "You must specify at least one id" }
		return agendaService.getAgendas(agendaIds.ids).map(agendaV2Mapper::map).injectReactorContext()
	}
}

