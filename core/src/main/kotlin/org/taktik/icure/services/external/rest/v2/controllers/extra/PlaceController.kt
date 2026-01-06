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
import org.taktik.icure.asyncservice.PlaceService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PlaceDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.mapper.PlaceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.collections.map

@RestController("placeControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/place")
@Tag(name = "place")
class PlaceController(
	private val placeService: PlaceService,
	private val placeV2Mapper: PlaceV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Create a Place")
	@PostMapping
	fun createPlace(
		@RequestBody placeDto: PlaceDto,
	): Mono<PlaceDto> = mono {
		placeV2Mapper.map(placeService.createPlace(placeV2Mapper.map(placeDto)))
	}

	@Operation(summary = "Create a batch of Place")
	@PostMapping("/batch")
	fun createPlaces(
		@RequestBody placeDtos: List<PlaceDto>,
	): Flux<PlaceDto> = placeService.createPlaces(
		placeDtos.map(placeV2Mapper::map)
	).map(placeV2Mapper::map).injectReactorContext()

	@Operation(summary = "Delete a batch of Places")
	@PostMapping("/delete/batch")
	fun deletePlaces(
		@RequestBody placeIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = placeIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
		placeService
			.deletePlaces(ids.map { IdAndRev(it, null) })
			.map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
			.injectReactorContext()
	}
		?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also {
			logger.error(it.message)
		}

	@Operation(summary = "Get a Place by id")
	@GetMapping("/{placeId}")
	fun getPlace(
		@PathVariable placeId: String,
	): Mono<PlaceDto> = mono {
		placeService.getPlace(placeId)?.let { placeV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Place fetching failed")
	}

	@Operation(summary = "Get multiple Places by their id")
	@PostMapping("/byIds")
	fun getPlacesByIds(
		@RequestBody placeIds: ListOfIdsDto,
	): Flux<PlaceDto> = placeService.getPlaces(placeIds.ids).map(placeV2Mapper::map).injectReactorContext()

	@Operation(summary = "Get all Places with pagination")
	@GetMapping
	fun getPlaces(
		@Parameter(description = "A MedicalLocation document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<PlaceDto> {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return placeService
			.getAllPlaces(offset)
			.mapElements(placeV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Modify a Place")
	@PutMapping
	fun modifyPlace(
		@RequestBody placeDto: PlaceDto,
	): Mono<PlaceDto> = mono {
		placeService.modifyPlace(placeV2Mapper.map(placeDto)).let { placeV2Mapper.map(it) }
	}

	@Operation(summary = "Modify a batch of Places")
	@PutMapping("/batch")
	fun modifyPlaces(
		@RequestBody placeDtos: List<PlaceDto>,
	): Flux<PlaceDto> = placeService.modifyPlaces(
		placeDtos.map(placeV2Mapper::map)
	).map(placeV2Mapper::map).injectReactorContext()

}
