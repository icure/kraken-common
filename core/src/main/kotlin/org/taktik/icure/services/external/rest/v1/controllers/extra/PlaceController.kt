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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.asyncservice.PlaceService
import org.taktik.icure.services.external.rest.v1.dto.PlaceDto
import org.taktik.icure.services.external.rest.v1.mapper.PlaceMapper
import org.taktik.icure.utils.injectReactorContext

@RestController
@Profile("app")
@RequestMapping("/rest/v1/place")
@Tag(name = "place")
class PlaceController(
	private val placeService: PlaceService,
	private val placeMapper: PlaceMapper
) {

	@Operation(summary = "Creates a place")
	@PostMapping
	fun createPlace(@RequestBody placeDto: PlaceDto) = mono {
		placeService.createPlace(placeMapper.map(placeDto))?.let { placeMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Place creation failed")
	}

	@Operation(summary = "Deletes a place")
	@DeleteMapping("/{placeIds}")
	fun deletePlace(@PathVariable placeIds: String) = placeService.deletePlace(placeIds.split(',')).injectReactorContext()

	@Operation(summary = "Gets a place")
	@GetMapping("/{placeId}")
	fun getPlace(@PathVariable placeId: String) = mono {
		placeService.getPlace(placeId)?.let { placeMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Place fetching failed")
	}

	@Operation(summary = "Gets all places with pagination")
	@GetMapping
	fun getPlaces() = placeService.getAllPlaces().map(placeMapper::map).injectReactorContext()

	@Operation(summary = "Modifies a place")
	@PutMapping
	fun modifyPlace(@RequestBody placeDto: PlaceDto) = mono {
		placeService.modifyPlace(placeMapper.map(placeDto))?.let { placeMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Place modification failed")
	}
}