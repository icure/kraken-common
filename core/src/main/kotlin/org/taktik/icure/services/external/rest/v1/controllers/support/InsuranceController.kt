/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

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
import org.taktik.icure.asyncservice.InsuranceService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.InsuranceDto
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.mapper.InsuranceMapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/insurance")
@Tag(name = "insurance")
class InsuranceController(
	private val insuranceService: InsuranceService,
	private val insuranceMapper: InsuranceMapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	@Operation(summary = "Gets all the insurances")
	@GetMapping
	fun getAllInsurances(
		@Parameter(description = "An insurance document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<InsuranceDto> {
		val paginationOffset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		return insuranceService
			.getAllInsurances(paginationOffset)
			.mapElements(insuranceMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Creates an insurance")
	@PostMapping
	fun createInsurance(
		@RequestBody insuranceDto: InsuranceDto,
	) = mono {
		val insurance =
			insuranceService.createInsurance(insuranceMapper.map(insuranceDto))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Insurance creation failed")

		insuranceMapper.map(insurance)
	}

	@Operation(summary = "Deletes an insurance")
	@DeleteMapping("/{insuranceId}")
	fun deleteInsurance(
		@PathVariable insuranceId: String,
	) = mono {
		insuranceService.deleteInsurance(insuranceId, null).let { DocIdentifierDto(it.id, it.rev) }
	}

	@Operation(summary = "Gets an insurance")
	@GetMapping("/{insuranceId}")
	fun getInsurance(
		@PathVariable insuranceId: String,
	) = mono {
		val insurance =
			insuranceService.getInsurance(insuranceId)
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Insurance fetching failed")
		insuranceMapper.map(insurance)
	}

	@Operation(summary = "Gets insurances by id")
	@PostMapping("/byIds")
	fun getInsurances(
		@RequestBody insuranceIds: ListOfIdsDto,
	): Flux<InsuranceDto> {
		val insurances = insuranceService.getInsurances(HashSet(insuranceIds.ids))
		return insurances.map { insuranceMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Gets an insurance")
	@GetMapping("/byCode/{insuranceCode}")
	fun listInsurancesByCode(
		@PathVariable insuranceCode: String,
	): Flux<InsuranceDto> {
		val insurances = insuranceService.listInsurancesByCode(insuranceCode)
		return insurances.map { insuranceMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Gets an insurance")
	@GetMapping("/byName/{insuranceName}")
	fun listInsurancesByName(
		@PathVariable insuranceName: String,
	): Flux<InsuranceDto> {
		val insurances = insuranceService.listInsurancesByName(insuranceName)

		return insurances.map { insuranceMapper.map(it) }.injectReactorContext()
	}

	@Operation(summary = "Modifies an insurance")
	@PutMapping
	fun modifyInsurance(
		@RequestBody insuranceDto: InsuranceDto,
	) = mono {
		val insurance =
			insuranceService.modifyInsurance(insuranceMapper.map(insuranceDto))
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Insurance modification failed")

		insuranceMapper.map(insurance)
	}
}
