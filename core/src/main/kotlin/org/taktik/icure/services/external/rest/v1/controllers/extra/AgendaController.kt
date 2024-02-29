/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.firstOrNull
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
import org.taktik.icure.asyncservice.AgendaService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.AgendaDto
import org.taktik.icure.services.external.rest.v1.mapper.AgendaMapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/agenda")
@Tag(name = "agenda")
class AgendaController(
	private val agendaService: AgendaService,
	private val agendaMapper: AgendaMapper,
	private val paginationConfig: SharedPaginationConfig
) {

	@Operation(summary = "Gets all agendas")
	@GetMapping
	fun getAgendas(
		@Parameter(description = "An agenda document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return agendaService
			.getAllAgendas(offset)
			.mapElements(agendaMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Creates a agenda")
	@PostMapping
	fun createAgenda(@RequestBody agendaDto: AgendaDto) = mono {
		val agenda = agendaService.createAgenda(agendaMapper.map(agendaDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agenda creation failed")

		agendaMapper.map(agenda)
	}

	@Operation(summary = "Delete agendas by id")
	@DeleteMapping("/{agendaIds}")
	fun deleteAgenda(@PathVariable agendaIds: String): Flux<DocIdentifier> {
		return agendaService.deleteAgendas(agendaIds.split(',').toSet()).injectReactorContext()
	}

	@Operation(summary = "Gets an agenda")
	@GetMapping("/{agendaId}")
	fun getAgenda(@PathVariable agendaId: String) = mono {
		val agenda = agendaService.getAgenda(agendaId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Agenda fetching failed")
		agendaMapper.map(agenda)
	}

	@Operation(summary = "Gets all agendas for user")
	@GetMapping("/byUser")
	fun getAgendasForUser(@RequestParam userId: String) = mono {
		agendaService.getAgendasByUser(userId).firstOrNull()?.let { agendaMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Agendas fetching failed")
	}

	@Operation(summary = "Gets readable agendas for user")
	@GetMapping("/readableForUser")
	fun getReadableAgendasForUser(@RequestParam userId: String): Flux<AgendaDto> {
		val agendas = agendaService.getReadableAgendaForUser(userId)
		return agendas.map { agendaMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Modifies an agenda")
	@PutMapping
	fun modifyAgenda(@RequestBody agendaDto: AgendaDto) = mono {
		val agenda = agendaService.modifyAgenda(agendaMapper.map(agendaDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agenda modification failed")

		agendaMapper.map(agenda)
	}
}
