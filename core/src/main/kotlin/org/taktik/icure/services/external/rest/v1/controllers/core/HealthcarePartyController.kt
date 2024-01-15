/*
 @Profile("app")
* Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
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
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.asyncservice.HealthcarePartyService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.exceptions.DeletionException
import org.taktik.icure.exceptions.DocumentNotFoundException
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.services.external.rest.v1.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v1.dto.PublicKeyDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v1.mapper.HealthcarePartyMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterChainMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterMapper
import org.taktik.icure.services.external.rest.v1.utils.paginatedList
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import org.taktik.icure.utils.warn
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/hcparty")
@Tag(name = "hcparty")
class HealthcarePartyController(
    private val filters: Filters,
    private val healthcarePartyService: HealthcarePartyService,
    private val sessionLogic: SessionInformationProvider,
    private val healthcarePartyMapper: HealthcarePartyMapper,
    private val filterChainMapper: FilterChainMapper,
	private val filterMapper: FilterMapper
) {

	private val healthcarePartyToHealthcarePartyDto = { it: HealthcareParty -> healthcarePartyMapper.map(it) }
	companion object {
		private val log: Logger = LoggerFactory.getLogger(this::class.java)
		private const val DEFAULT_LIMIT = 1000
	}

	@Operation(summary = "Get the current healthcare party if logged in.", description = "General information about the current healthcare Party")
	@GetMapping("/current")
	fun getCurrentHealthcareParty() = mono {
		// If the current user is not an HCP this method gives a 401. 400 or 404 would be more appropriate, however, some clients have business logic relying on this error, so it is better to not change this...
		val healthcareParty = healthcarePartyService.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the current healthcare party. Probable reasons: no healthcare party is logged in, or server error. Please try again or read the server log.")
		healthcarePartyMapper.map(healthcareParty)
	}

	@Operation(summary = "List healthcare parties with(out) pagination", description = "Returns a list of healthcare parties.")
	@GetMapping
	fun listHealthcareParties(
		@Parameter(description = "A healthcare party Last name") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A healthcare party document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?
	) = mono {

		val realLimit = limit ?: DEFAULT_LIMIT
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, realLimit + 1)

		healthcarePartyService.findHealthcarePartiesBy(paginationOffset, desc).paginatedList(healthcarePartyToHealthcarePartyDto, realLimit)
	}

	@Operation(summary = "Find healthcare parties by name with(out) pagination", description = "Returns a list of healthcare parties.")
	@GetMapping("/byName")
	fun findByName(
		@Parameter(description = "The Last name search value") @RequestParam(required = false) name: String?,
		@Parameter(description = "A healthcare party Last name") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A healthcare party document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?
	) = mono {

		val realLimit = limit ?: DEFAULT_LIMIT
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, realLimit + 1)
		if (name.isNullOrEmpty()) {
			healthcarePartyService.findHealthcarePartiesBy(paginationOffset, desc)
		} else {
			healthcarePartyService.findHealthcarePartiesBy(name, paginationOffset, desc)
		}.paginatedList(healthcarePartyToHealthcarePartyDto, realLimit)
	}

	@Operation(summary = "Find healthcare parties by nihii or ssin with(out) pagination", description = "Returns a list of healthcare parties.")
	@GetMapping("/byNihiiOrSsin/{searchValue}")
	fun findBySsinOrNihii(
		@PathVariable searchValue: String,
		@Parameter(description = "A healthcare party Last name") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A healthcare party document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean
	) = mono {

		val realLimit = limit ?: DEFAULT_LIMIT
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, realLimit + 1)

		healthcarePartyService.findHealthcarePartiesBySsinOrNihii(searchValue, paginationOffset, desc)
			.paginatedList(healthcarePartyToHealthcarePartyDto, realLimit)
	}

	@Operation(summary = "Find healthcare parties by name with(out) pagination", description = "Returns a list of healthcare parties.")
	@GetMapping("/byNameStrict/{name}")
	fun listByName(
		@Parameter(description = "The Last name search value")
		@PathVariable name: String
	) =
		healthcarePartyService.listHealthcarePartiesByName(name)
			.map { healthcarePartyMapper.map(it) }
			.injectReactorContext()

	@Operation(summary = "Find healthcare parties by name with(out) pagination", description = "Returns a list of healthcare parties.")
	@GetMapping("/bySpecialityAndPostCode/{type}/{spec}/{firstCode}/to/{lastCode}")
	fun findBySpecialityAndPostCode(
		@Parameter(description = "The type of the HCP (persphysician)") @PathVariable type: String,
		@Parameter(description = "The speciality of the HCP") @PathVariable spec: String,
		@Parameter(description = "The first postCode for the HCP") @PathVariable firstCode: String,
		@Parameter(description = "The last postCode for the HCP") @PathVariable lastCode: String,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int
	) = mono {
		healthcarePartyService.listHealthcarePartiesBySpecialityAndPostcode(type, spec, firstCode, lastCode).paginatedList(healthcarePartyToHealthcarePartyDto, limit)
	}

	@Operation(summary = "Create a healthcare party", description = "One of Name or Last name+First name, Nihii, and Public key are required.")
	@PostMapping
	fun createHealthcareParty(@RequestBody h: HealthcarePartyDto) = mono {
		val hcParty = try {
			healthcarePartyService.createHealthcareParty(healthcarePartyMapper.map(h))
		} catch (e: MissingRequirementsException) {
			log.warn(e) { e.message }
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}

		val succeed = hcParty != null
		if (succeed) {
			hcParty?.let { healthcarePartyMapper.map(it) }
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Healthcare party creation failed.")
		}
	}


	@Suppress("DEPRECATION")
	@Deprecated(message = "A HCP may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	@Operation(
		summary = "Get the HcParty encrypted AES keys indexed by owner. As a HCp may now have multiple AES keys, this service is deprecated. Use /{healthcarePartyId}/aesExchangeKeys",
		description = "(key, value) of the map is as follows: (ID of the owner of the encrypted AES key, encrypted AES key)",
		deprecated = true
	)
	@GetMapping("/{healthcarePartyId}/keys")
	fun getHcPartyKeysForDelegate(@PathVariable healthcarePartyId: String) = mono {
		healthcarePartyService.getHcPartyKeysForDelegate(healthcarePartyId)
	}

	@Operation(
		summary = "Get the HcParty encrypted AES keys indexed by owner.",
		description = "(key, value) of the map is as follows: (ID of the owner of the encrypted AES key, encrypted AES keys)"
	)
	@GetMapping("/{healthcarePartyId}/aesExchangeKeys")
	fun getAesExchangeKeysForDelegate(@PathVariable healthcarePartyId: String) = mono {
		healthcarePartyService.getAesExchangeKeysForDelegate(healthcarePartyId)
	}

	@Operation(summary = "Get a healthcareParty by his ID", description = "General information about the healthcare Party")
	@GetMapping("/{healthcarePartyId}")
	fun getHealthcareParty(@PathVariable healthcarePartyId: String) = mono {
		val healthcareParty = healthcarePartyService.getHealthcareParty(healthcarePartyId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the healthcare party. Probable reasons: no such party exists, or server error. Please try again or read the server log.")
		healthcarePartyMapper.map(healthcareParty)
	}

	@Operation(summary = "Get healthcareParties by their IDs", description = "General information about the healthcare Party")
	@GetMapping("/byIds/{healthcarePartyIds}")
	fun getHealthcareParties(@PathVariable healthcarePartyIds: String) =
		healthcarePartyService.getHealthcareParties(healthcarePartyIds.split(','))
			.map { healthcarePartyMapper.map(it) }
			.injectReactorContext()

	@Operation(summary = "Find children of an healthcare parties", description = "Return a list of children hcp.")
	@GetMapping("/{parentId}/children")
	fun getHealthcarePartiesByParentId(@PathVariable parentId: String) =
		healthcarePartyService.getHealthcarePartiesByParentId(parentId)
			.map { healthcarePartyMapper.map(it) }
			.injectReactorContext()

	@Operation(summary = "Get public key of a healthcare party", description = "Returns the public key of a healthcare party in Hex")
	@GetMapping("/{healthcarePartyId}/publicKey")
	fun getPublicKey(@PathVariable healthcarePartyId: String) = mono {
		val publicKey = try {
			healthcarePartyService.getPublicKey(healthcarePartyId)
		} catch (e: DocumentNotFoundException) {
			log.warn(e.message, e)
			throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
		} ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No public key is found.")

		PublicKeyDto(healthcarePartyId, publicKey)
	}

	@Operation(summary = "Delete a healthcare party", description = "Deleting a healthcareParty. Response is an array containing the id of deleted healthcare party.")
	@DeleteMapping("/{healthcarePartyIds}")
	fun deleteHealthcareParties(@PathVariable healthcarePartyIds: String): Flux<DocIdentifier> = flow {
		try {
			emitAll(healthcarePartyService.deleteHealthcareParties(healthcarePartyIds.split(',')))
		} catch (e: DeletionException) {
			log.warn(e) { e.message }
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
		}
	}.injectReactorContext()

	@Operation(summary = "Modify a Healthcare Party.", description = "No particular return value. It's just a message.")
	@PutMapping
	fun modifyHealthcareParty(@RequestBody healthcarePartyDto: HealthcarePartyDto) = mono {
		healthcarePartyService.modifyHealthcareParty(healthcarePartyMapper.map(healthcarePartyDto))?.let {
			healthcarePartyMapper.map(it)
		} ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot find Healthcare Party.")
	}

	@Operation(summary = "Filter healthcare parties for the current user (HcParty)", description = "Returns a list of healthcare party along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterHealthPartiesBy(
		@Parameter(description = "A HealthcareParty document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<HealthcarePartyDto>
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)
		val healthcareParties = healthcarePartyService.filterHealthcareParties(paginationOffset, filterChainMapper.tryMap(filterChain).orThrow())

		healthcareParties.paginatedList(healthcarePartyToHealthcarePartyDto, realLimit)
	}

	@Operation(summary = "Get ids of healthcare party matching the provided filter for the current user (HcParty) ")
	@PostMapping("/match")
	fun matchHealthcarePartiesBy(@RequestBody filter: AbstractFilterDto<HealthcarePartyDto>) = mono {
		filters.resolve(filterMapper.tryMap(filter).orThrow()).toList()
	}
}
