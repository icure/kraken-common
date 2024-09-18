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
import org.taktik.couchdb.entity.IdAndRev

import org.taktik.icure.asyncservice.PlaceService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PlaceDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.PlaceV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("placeControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/place")
@Tag(name = "place")
class PlaceController(
	private val placeService: PlaceService,
	private val placeV2Mapper: PlaceV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val paginationConfig: SharedPaginationConfig
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Creates a place")
	@PostMapping
	fun createPlace(@RequestBody placeDto: PlaceDto) = mono {
		placeService.createPlace(placeV2Mapper.map(placeDto))?.let { placeV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Place creation failed")
	}

	@Operation(summary = "Deletes multiple Places")
	@PostMapping("/delete/batch")
	fun deletePlaces(@RequestBody placeIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		placeService.deletePlaces(
			placeIds.ids.map { IdAndRev(it, null) }
		).map(docIdentifierV2Mapper::map).injectReactorContext()

	@Operation(summary = "Deletes a multiple Places if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deletePlacesWithRev(@RequestBody placeIds: ListOfIdsAndRevDto): Flux<DocIdentifierDto> =
		placeService.deletePlaces(
			placeIds.ids.map(idWithRevV2Mapper::map)
		).map(docIdentifierV2Mapper::map).injectReactorContext()

	@Operation(summary = "Deletes an Place")
	@DeleteMapping("/{placeId}")
	fun deletePlace(
		@PathVariable placeId: String,
		@Parameter(required = false) rev: String? = null
	): Mono<DocIdentifierDto> = mono {
		placeService.deletePlace(placeId, rev).let(docIdentifierV2Mapper::map)
	}

	@PostMapping("/undelete/{placeId}")
	fun undeletePlace(
		@PathVariable placeId: String,
		@Parameter(required=true) rev: String
	): Mono<PlaceDto> = mono {
		placeV2Mapper.map(placeService.undeletePlace(placeId, rev))
	}

	@DeleteMapping("/purge/{placeId}")
	fun purgePlace(
		@PathVariable placeId: String,
		@Parameter(required=true) rev: String
	): Mono<DocIdentifierDto> = mono {
		placeService.purgePlace(placeId, rev).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Gets a place")
	@GetMapping("/{placeId}")
	fun getPlace(@PathVariable placeId: String) = mono {
		placeService.getPlace(placeId)?.let { placeV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Place fetching failed")
	}

	@Operation(summary = "Gets all places with pagination")
	@GetMapping
	fun getPlaces(
		@Parameter(description = "A MedicalLocation document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux<PlaceDto> {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return placeService
			.getAllPlaces(offset)
			.mapElements(placeV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Modifies an place")
	@PutMapping
	fun modifyPlace(@RequestBody placeDto: PlaceDto) = mono {
		placeService.modifyPlace(placeV2Mapper.map(placeDto))?.let { placeV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Place modification failed")
	}
}