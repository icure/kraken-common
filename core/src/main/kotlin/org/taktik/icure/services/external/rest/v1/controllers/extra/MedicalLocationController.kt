/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.taktik.icure.asyncservice.MedicalLocationService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.MedicalLocationDto
import org.taktik.icure.services.external.rest.v1.mapper.MedicalLocationMapper
import org.taktik.icure.utils.injectReactorContext

@RestController
@Profile("app")
@RequestMapping("/rest/v1/medicallocation")
@Tag(name = "medicallocation")
class MedicalLocationController(
	private val medicalLocationService: MedicalLocationService,
	private val medicalLocationMapper: MedicalLocationMapper,
	private val paginationConfig: SharedPaginationConfig
) {

	@Operation(summary = "Creates a medical location")
	@PostMapping
	fun createMedicalLocation(@RequestBody medicalLocationDto: MedicalLocationDto) = mono {
		medicalLocationService.createMedicalLocation(medicalLocationMapper.map(medicalLocationDto))?.let { medicalLocationMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Medical location creation failed")
	}

	@Operation(summary = "Deletes a medical location")
	@DeleteMapping("/{locationIds}")
	fun deleteMedicalLocation(@PathVariable locationIds: String) =
		medicalLocationService.deleteMedicalLocations(locationIds.split(',')).injectReactorContext()

	@Operation(summary = "Gets a medical location")
	@GetMapping("/{locationId}")
	fun getMedicalLocation(@PathVariable locationId: String) = mono {
		medicalLocationService.getMedicalLocation(locationId)?.let { medicalLocationMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "medical location fetching failed")
	}

	@Operation(summary = "Gets all medical locations with pagination")
	@GetMapping
	fun getMedicalLocations(
		@Parameter(description = "A MedicalLocation document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return medicalLocationService
			.getAllMedicalLocations(offset)
			.mapElements(medicalLocationMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Modifies a medical location")
	@PutMapping
	fun modifyMedicalLocation(@RequestBody medicalLocationDto: MedicalLocationDto) = mono {
		medicalLocationService.modifyMedicalLocation(medicalLocationMapper.map(medicalLocationDto))?.let { medicalLocationMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "medical location modification failed")
	}
}