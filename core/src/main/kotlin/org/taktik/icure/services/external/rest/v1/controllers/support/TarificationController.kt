/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncservice.TarificationService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.TarificationDto
import org.taktik.icure.services.external.rest.v1.mapper.TarificationMapper
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/tarification")
@Tag(name = "tarification")
class TarificationController(
	private val tarificationService: TarificationService,
	private val tarificationMapper: TarificationMapper,
	private val objectMapper: ObjectMapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	@Operation(
		summary = "Finding tarifications by tarification, type and version with pagination.",
		description = "Returns a list of tarifications matched with given input.",
	)
	@GetMapping("/byLabel")
	fun findPaginatedTarificationsByLabel(
		@RequestParam(required = false) region: String?,
		@RequestParam(required = false) types: String?,
		@RequestParam(required = false) language: String?,
		@RequestParam(required = false) label: String?,
		@RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A tarification document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<TarificationDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		return tarificationService
			.findTarificationsOfTypesByLabel(
				region,
				language,
				label,
				types?.split(',')?.toSet(),
				PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit),
			).mapElements(tarificationMapper::map)
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Finding tarifications by tarification, type and version with pagination.",
		description = "Returns a list of tarifications matched with given input.",
	)
	@GetMapping
	fun findPaginatedTarifications(
		@RequestParam(required = false) region: String?,
		@RequestParam(required = false) type: String?,
		@RequestParam(required = false) tarification: String?,
		@RequestParam(required = false) version: String?,
		@Parameter(description = "A tarification document ID") @RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<TarificationDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		return tarificationService
			.findTarificationsBy(
				region,
				type,
				tarification,
				version,
				PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit),
			).mapElements(tarificationMapper::map)
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Finding tarifications by tarification, type and version",
		description = "Returns a list of tarifications matched with given input.",
	)
	@GetMapping("/byRegionTypeTarification")
	fun findTarifications(
		@Parameter(description = "Tarification region") @RequestParam(required = false) region: String?,
		@Parameter(description = "Tarification type") @RequestParam(required = false) type: String?,
		@Parameter(description = "Tarification tarification") @RequestParam(required = false) tarification: String?,
		@Parameter(description = "Tarification version") @RequestParam(required = false) version: String?,
	): Flux<TarificationDto> = tarificationService
		.findTarificationsBy(region, type, tarification, version)
		.map {
			tarificationMapper.map(it)
		}.injectReactorContext()

	@Operation(summary = "Create a Tarification", description = "Type, Tarification and Version are required.")
	@PostMapping
	fun createTarification(
		@RequestBody c: TarificationDto,
	) = mono {
		tarificationService.createTarification(tarificationMapper.map(c))?.let { tarificationMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tarification creation failed.")
	}

	@Operation(summary = "Get a list of tarifications by ids", description = "Keys must be delimited by coma")
	@PostMapping("/byIds")
	fun getTarifications(
		@RequestBody tarificationIds: ListOfIdsDto,
	) = tarificationService.getTarifications(tarificationIds.ids).map { f -> tarificationMapper.map(f) }.injectReactorContext()

	@Operation(
		summary = "Get a tarification",
		description = "Get a tarification based on ID or (tarification,type,version) as query strings. (tarification,type,version) is unique.",
	)
	@GetMapping("/{tarificationId}")
	fun getTarification(
		@Parameter(description = "Tarification id") @PathVariable tarificationId: String,
	) = mono {
		tarificationService.getTarification(tarificationId)?.let { tarificationMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the tarification. Read the app logs.")
	}

	@Operation(
		summary = "Get a tarification",
		description = "Get a tarification based on ID or (tarification,type,version) as query strings. (tarification,type,version) is unique.",
	)
	@GetMapping("/{type}/{tarification}/{version}")
	fun getTarificationWithParts(
		@Parameter(description = "Tarification type", required = true) @PathVariable type: String,
		@Parameter(description = "Tarification tarification", required = true) @PathVariable tarification: String,
		@Parameter(description = "Tarification version", required = true) @PathVariable version: String,
	) = mono {
		tarificationService.getTarification(type, tarification, version)?.let { tarificationMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the tarification. Read the app logs.")
	}

	@Operation(summary = "Modify a tarification", description = "Modification of (type, tarification, version) is not allowed.")
	@PutMapping
	fun modifyTarification(
		@RequestBody tarificationDto: TarificationDto,
	) = mono {
		try {
			tarificationService.modifyTarification(tarificationMapper.map(tarificationDto))?.let { tarificationMapper.map(it) }
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Modification of the tarification failed. Read the server log.")
		} catch (e: Exception) {
			throw ResponseStatusException(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"A problem regarding modification of the tarification. Read the app logs: ",
			)
		}
	}
}
