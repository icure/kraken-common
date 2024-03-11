/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.taktik.icure.asyncservice.MedicalLocationService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.MedicalLocationDto
import org.taktik.icure.services.external.rest.v2.mapper.MedicalLocationV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController("medicalLocationControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/medicallocation")
@Tag(name = "medicalLocation")
class MedicalLocationController(
	private val medicalLocationService: MedicalLocationService,
	private val medicalLocationV2Mapper: MedicalLocationV2Mapper,
	private val paginationConfig: SharedPaginationConfig
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Creates a medical location")
	@PostMapping
	fun createMedicalLocation(@RequestBody medicalLocationDto: MedicalLocationDto) = mono {
		medicalLocationService.createMedicalLocation(medicalLocationV2Mapper.map(medicalLocationDto))?.let { medicalLocationV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Medical location creation failed")
	}

	@Operation(summary = "Deletes medical locations")
	@PostMapping("/delete/batch")
	fun deleteMedicalLocations(@RequestBody locationIds: ListOfIdsDto): Flux<DocIdentifier> =
		locationIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
			medicalLocationService.deleteMedicalLocations(ids).injectReactorContext()
		} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also { logger.error(it.message) }

	@Operation(summary = "Gets a medical location")
	@GetMapping("/{locationId}")
	fun getMedicalLocation(@PathVariable locationId: String) = mono {
		medicalLocationService.getMedicalLocation(locationId)?.let { medicalLocationV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "medical location fetching failed")
	}

	@Operation(summary = "Gets all medical locations with pagination")
	@GetMapping
	fun getMedicalLocations(
		@Parameter(description = "A MedicalLocation document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux<MedicalLocationDto> {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return medicalLocationService
			.getAllMedicalLocations(offset)
			.mapElements(medicalLocationV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Modifies a medical location")
	@PutMapping
	fun modifyMedicalLocation(@RequestBody medicalLocationDto: MedicalLocationDto) = mono {
		medicalLocationService.modifyMedicalLocation(medicalLocationV2Mapper.map(medicalLocationDto))?.let { medicalLocationV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "medical location modification failed")
	}
}