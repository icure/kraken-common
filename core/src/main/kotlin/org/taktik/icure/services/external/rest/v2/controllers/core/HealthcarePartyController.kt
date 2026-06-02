/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
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
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.customentities.config.StandardRootEntitiesExtensionConfig
import org.taktik.icure.customentities.util.CachedCustomEntitiesConfigurationProvider
import org.taktik.icure.customentities.util.ExtendableBuiltinEntityValidatorMapperConfigsProvider
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.errorreporting.MapperScopePathProvider
import org.taktik.icure.services.external.rest.v2.mapper.MappersWithCustomExtensions.mapFromDtoWithExtension
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.HealthcarePartyService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.exceptions.DocumentNotFoundException
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.PublicKeyDto
import org.taktik.icure.services.external.rest.v2.dto.conflicts.ConflictResolutionRequestDto
import org.taktik.icure.services.external.rest.v2.dto.conflicts.ConflictResolutionResultDto
import org.taktik.icure.services.external.rest.v2.dto.conflicts.MergeResultDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEncryptionKeypairIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.HexStringDto
import org.taktik.icure.services.external.rest.v2.mapper.HealthcarePartyV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.conflicts.ConflictResolutionV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.conflicts.MergeResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("healthcarePartyControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/hcparty")
@Tag(name = "healthcareParty")
class HealthcarePartyController(
	private val healthcarePartyService: HealthcarePartyService,
	private val sessionLogic: SessionInformationProvider,
	private val healthcarePartyV2Mapper: HealthcarePartyV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val objectMapper: ObjectMapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val conflictResolutionV2Mapper: ConflictResolutionV2Mapper,
	private val mergeResultV2Mapper: MergeResultV2Mapper,
	private val customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
	private val scopePathProvider: MapperScopePathProvider,
	private val builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
) {
	companion object {
		private val logger: Logger = LoggerFactory.getLogger(this::class.java)
	}

	private suspend fun HealthcarePartyDto.toDomain(): HealthcareParty =
		mapFromDtoWithExtension(
			this,
			customEntitiesConfigurationProvider,
			StandardRootEntitiesExtensionConfig::healthcareParty,
			healthcarePartyV2Mapper::map,
			scopePathProvider.getScopePathFor("HealthcareParty"),
			builtinValidationConfigsProvider,
		)

	private suspend fun List<HealthcarePartyDto>.toDomain(): List<HealthcareParty> =
		mapFromDtoWithExtension(
			this,
			customEntitiesConfigurationProvider,
			StandardRootEntitiesExtensionConfig::healthcareParty,
			healthcarePartyV2Mapper::map,
			scopePathProvider.getScopePathFor("HealthcareParty"),
			builtinValidationConfigsProvider,
		)

	private fun HealthcareParty.toDto(): HealthcarePartyDto = healthcarePartyV2Mapper.map(this)

	private fun Flow<HealthcareParty>.toDto(): Flow<HealthcarePartyDto> = map { it.toDto() }

	@JvmName("toDtoPagination")
	private fun Flow<PaginationElement>.toDto(): Flow<PaginationElement> =
		mapElements<HealthcareParty, HealthcarePartyDto> { it.toDto() }

	private fun toDtoLambda(): (HealthcareParty) -> HealthcarePartyDto = { it.toDto() }

	@Operation(
		summary = "Get the current healthcare party if logged in.",
		description = "General information about the current healthcare Party",
	)
	@GetMapping("/current")
	fun getCurrentHealthcareParty(): Mono<HealthcarePartyDto> = mono {
		val healthcareParty =
			healthcarePartyService.getHealthcareParty(requireNotNull(sessionLogic.getCurrentSessionContext().getHealthcarePartyId()) {
				"Current user is not a healthcare party."
			})
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"A problem regarding fetching the current healthcare party. Probable reasons: no healthcare party is logged in, or server error. Please try again or read the server log.",
				)
		healthcareParty.toDto()
	}

	@Operation(
		summary = "List healthcare parties with pagination",
		description = "Returns a list of healthcare parties.",
	)
	@GetMapping
	fun findHealthcarePartiesBy(
		@Parameter(description = "A healthcare party Last name") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A healthcare party document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?,
	): PaginatedFlux<HealthcarePartyDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return healthcarePartyService
			.findHealthcarePartiesBy(paginationOffset, desc)
			.toDto()
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Find healthcare parties by name with pagination",
		description = "Returns a list of healthcare parties.",
	)
	@GetMapping("/byName")
	fun findHealthcarePartiesByName(
		@Parameter(description = "The Last name search value") @RequestParam(required = false) name: String?,
		@Parameter(description = "A healthcare party Last name") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A healthcare party document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?,
	): PaginatedFlux<HealthcarePartyDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return if (name.isNullOrEmpty()) {
			healthcarePartyService.findHealthcarePartiesBy(paginationOffset, desc)
		} else {
			healthcarePartyService.findHealthcarePartiesBy(name, paginationOffset, desc)
		}.toDto().asPaginatedFlux()
	}

	@Operation(
		summary = "Find healthcare parties by nihii or ssin with pagination",
		description = "Returns a list of healthcare parties.",
	)
	@GetMapping("/byNihiiOrSsin/{searchValue}")
	fun findHealthcarePartiesBySsinOrNihii(
		@PathVariable searchValue: String,
		@Parameter(description = "A healthcare party Last name") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A healthcare party document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Descending") @RequestParam(required = false, defaultValue = "false") desc: Boolean,
	): PaginatedFlux<HealthcarePartyDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return healthcarePartyService
			.findHealthcarePartiesBySsinOrNihii(searchValue, paginationOffset, desc)
			.toDto()
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Find healthcare parties by name without pagination",
		description = "Returns a list of healthcare parties.",
	)
	@GetMapping("/byNameStrict/{name}")
	fun listHealthcarePartiesByName(
		@Parameter(description = "The Last name search value")
		@PathVariable name: String,
	): Flux<HealthcarePartyDto> = healthcarePartyService
		.listHealthcarePartiesByName(name)
		.toDto()
		.injectReactorContext()

	@Operation(
		summary = "Find healthcare parties by name with pagination",
		description = "Returns a paginated list of healthcare parties.",
	)
	@GetMapping("/bySpecialityAndPostCode/{type}/{spec}/{firstCode}/to/{lastCode}")
	fun findHealthcarePartiesBySpecialityAndPostCode(
		@Parameter(description = "The type of the HCP (persphysician)") @PathVariable type: String,
		@Parameter(description = "The speciality of the HCP") @PathVariable spec: String,
		@Parameter(description = "The first postCode for the HCP") @PathVariable firstCode: String,
		@Parameter(description = "The last postCode for the HCP") @PathVariable lastCode: String,
		@Parameter(description = "A healthcare party Last name") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A healthcare party document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<HealthcarePartyDto> {
		val key = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(key, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return healthcarePartyService
			.listHealthcarePartiesBySpecialityAndPostcode(type, spec, firstCode, lastCode, paginationOffset)
			.toDto()
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Create a HealthcareParty",
		description = "One of Name or Last name+First name, Nihii, and Public key are required.",
	)
	@PostMapping
	fun createHealthcareParty(
		@RequestBody h: HealthcarePartyDto,
	): Mono<HealthcarePartyDto> = mono {
		val hcParty = healthcarePartyService.createHealthcareParty(h.toDomain())
		hcParty.toDto()
	}

	@Operation(
		summary = "Create a batch of HealthcareParty",
		description = "One of Name or Last name+First name, Nihii, and Public key are required for each HealthcareParty.",
	)
	@PostMapping("/batch")
	fun createHealthcareParties(
		@RequestBody healthcareParties: List<HealthcarePartyDto>,
	): Flux<HealthcarePartyDto> = flow {
		emitAll(healthcarePartyService.createHealthcareParties(healthcareParties.toDomain()).toDto())
	}.injectReactorContext()

	@Operation(
		summary = "Get the HcParty encrypted AES keys indexed by owner.",
		description = "(key, value) of the map is as follows: (ID of the owner of the encrypted AES key, encrypted AES keys)",
	)
	@GetMapping("/{healthcarePartyId}/aesExchangeKeys")
	fun getAesExchangeKeysForDelegate(
		@PathVariable healthcarePartyId: String,
	): Mono<Map<String, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>>> = mono {
		healthcarePartyService.getAesExchangeKeysForDelegate(healthcarePartyId)
	}

	@Operation(
		summary = "Get a healthcareParty by his ID",
		description = "General information about the healthcare Party",
	)
	@GetMapping("/{healthcarePartyId}")
	fun getHealthcareParty(
		@PathVariable healthcarePartyId: String,
	): Mono<HealthcarePartyDto> = mono {
		val healthcareParty =
			healthcarePartyService.getHealthcareParty(healthcarePartyId)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"A problem regarding fetching the healthcare party. Probable reasons: no such party exists, or server error. Please try again or read the server log.",
				)
		healthcareParty.toDto()
	}

	@Operation(
		summary = "Get healthcareParties by their IDs",
		description = "General information about the healthcare Party",
	)
	@PostMapping("/byIds")
	fun getHealthcareParties(
		@RequestBody healthcarePartyIds: ListOfIdsDto,
	): Flux<HealthcarePartyDto> = healthcarePartyIds.ids
		.takeIf { it.isNotEmpty() }
		?.let { ids ->
			healthcarePartyService
				.getHealthcareParties(ids)
				.toDto()
				.injectReactorContext()
		}
		?: throw ResponseStatusException(
			HttpStatus.BAD_REQUEST,
			"A required query parameter was not specified for this request.",
		).also { logger.error(it.message) }

	@Operation(summary = "Find children of an healthcare parties", description = "Return a list of children hcp.")
	@GetMapping("/{parentId}/children")
	fun listHealthcarePartiesByParentId(
		@PathVariable parentId: String,
	): Flux<HealthcarePartyDto> = healthcarePartyService
		.getHealthcarePartiesByParentId(parentId)
		.toDto()
		.injectReactorContext()

	@Operation(
		summary = "Get public key of a healthcare party",
		description = "Returns the public key of a healthcare party in Hex",
	)
	@GetMapping("/{healthcarePartyId}/publicKey")
	fun getPublicKey(
		@PathVariable healthcarePartyId: String,
	): Mono<PublicKeyDto> = mono {
		val publicKey =
			try {
				healthcarePartyService.getPublicKey(healthcarePartyId)
			} catch (e: DocumentNotFoundException) {
				logger.warn(e.message, e)
				throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
			} ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No public key is found.")

		PublicKeyDto(healthcarePartyId, publicKey)
	}

	@Operation(summary = "Deletes multiple HealthcareParties")
	@PostMapping("/delete/batch")
	fun deleteHealthcareParties(
		@RequestBody healthcarePartyIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = healthcarePartyService
		.deleteHealthcareParties(
			healthcarePartyIds.ids.map { IdAndRev(it, null) },
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Deletes multiple HealthcareParties if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteHealthcarePartiesWithRev(
		@RequestBody healthcarePartyIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = healthcarePartyService
		.deleteHealthcareParties(
			healthcarePartyIds.ids.map(idWithRevV2Mapper::map),
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Deletes an HealthcareParty")
	@DeleteMapping("/{healthcarePartyId}")
	fun deleteHealthcareParty(
		@PathVariable healthcarePartyId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		healthcarePartyService.deleteHealthcareParty(healthcarePartyId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{healthcarePartyId}")
	fun undeleteHealthcareParty(
		@PathVariable healthcarePartyId: String,
		@RequestParam(required = true) rev: String,
	): Mono<HealthcarePartyDto> = reactorCacheInjector.monoWithCachedContext(10) {
		healthcarePartyService.undeleteHealthcareParty(healthcarePartyId, rev).toDto()
	}

	@PostMapping("/undelete/batch")
	fun undeleteHealthcareParties(
		@RequestBody healthcarePartyIds: ListOfIdsAndRevDto,
	): Flux<HealthcarePartyDto> = healthcarePartyService
		.undeleteHealthcareParties(
			healthcarePartyIds.ids.map(idWithRevV2Mapper::map),
		).toDto()
		.injectCachedReactorContext(reactorCacheInjector, 100)


	@DeleteMapping("/purge/{healthcarePartyId}")
	fun purgeHealthcareParty(
		@PathVariable healthcarePartyId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		healthcarePartyService.purgeHealthcareParty(healthcarePartyId, rev).let(docIdentifierV2Mapper::map)
	}

	@PostMapping("/purge/batch")
	fun purgeHealthcareParties(
		@RequestBody healthcarePartyIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = healthcarePartyService
		.purgeHealthcareParties(
			healthcarePartyIds.ids.map(idWithRevV2Mapper::map),
		).map(docIdentifierV2Mapper::map)
		.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "Modify a HealthcareParty.", description = "No particular return value. It's just a message.")
	@PutMapping
	fun modifyHealthcareParty(
		@RequestBody healthcarePartyDto: HealthcarePartyDto,
	): Mono<HealthcarePartyDto> = mono {
		healthcarePartyService.modifyHealthcareParty(healthcarePartyDto.toDomain()).toDto()
	}

	@Operation(summary = "Modify a batch of HealthcareParty.")
	@PutMapping("/batch")
	fun modifyHealthcareParties(
		@RequestBody healthcareParties: List<HealthcarePartyDto>,
	): Flux<HealthcarePartyDto> = flow {
		emitAll(healthcarePartyService.modifyHealthcareParties(healthcareParties.toDomain()).toDto())
	}.injectReactorContext()

	@Operation(summary = "Get the ids of the HealthcareParties matching the provided filter.")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchHealthcarePartiesBy(
		@RequestBody filter: AbstractFilterDto<HealthcarePartyDto>,
	): Flux<String> = healthcarePartyService
		.matchHealthcarePartiesBy(
			filter = filterV2Mapper.tryMap(filter).orThrow(),
		).injectReactorContext()

	@Operation(
		summary = "Filter healthcare parties for the current user (HcParty)",
		description = "Returns a list of healthcare party along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.",
	)
	@PostMapping("/filter")
	fun filterHealthPartiesBy(
		@Parameter(description = "A HealthcareParty document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<HealthcarePartyDto>,
	): Mono<PaginatedList<HealthcarePartyDto>> = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)
		val healthcareParties =
			healthcarePartyService.filterHealthcareParties(paginationOffset, filterChainV2Mapper.tryMap(filterChain).orThrow())

		healthcareParties.paginatedList(toDtoLambda(), realLimit, objectMapper = objectMapper)
	}

	@GetMapping("/conflicts", produces = [APPLICATION_JSON_VALUE])
	fun getConflictingEntitiesIds(): Flux<String> =
		healthcarePartyService.getConflictingEntitiesIds().injectReactorContext()

	@GetMapping("/conflicts/{entityId}")
	fun getConflictsForEntity(
		@PathVariable entityId: String,
	): Flux<HealthcarePartyDto> =
		healthcarePartyService.getConflictsFor(entityId)
			.toDto()
			.injectReactorContext()

	@PostMapping("/conflicts/winner")
	fun declareConflictWinner(
		@RequestBody request: ConflictResolutionRequestDto<HealthcarePartyDto>
	): Mono<ConflictResolutionResultDto<HealthcarePartyDto>> = mono {
		val result = healthcarePartyService.declareConflictWinner(
			entity = request.document.toDomain(),
			conflictsToPurge = request.conflictsToPurge
		)
		conflictResolutionV2Mapper.map(result) { it.toDto() }
	}

	@PostMapping("/conflicts/solve")
	fun autoSolveConflicts(
		@RequestBody entityIds: List<String>
	): Flux<MergeResultDto> = healthcarePartyService
		.solveConflicts(limit = null, ids = entityIds)
		.map(mergeResultV2Mapper::map)
		.injectReactorContext()
}
