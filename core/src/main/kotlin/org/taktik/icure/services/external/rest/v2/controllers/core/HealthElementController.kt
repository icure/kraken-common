/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
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
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.HealthElementService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.services.external.rest.v2.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v2.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.HealthElementV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.StubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.HealthElementBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("healthElementControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/helement")
@Tag(name = "healthElement")
class HealthElementController(
	private val healthElementService: HealthElementService,
	private val healthElementV2Mapper: HealthElementV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val stubV2Mapper: StubV2Mapper,
	private val bulkShareResultV2Mapper: HealthElementBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val objectMapper: ObjectMapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(
		summary = "Create a health element with the current user",
		description = "Returns an instance of created health element.",
	)
	@PostMapping
	fun createHealthElement(
		@RequestBody c: HealthElementDto,
	): Mono<HealthElementDto> = mono {
		healthElementV2Mapper.map(healthElementService.createHealthElement(healthElementV2Mapper.map(c)))
	}

	@Operation(summary = "Get a health element")
	@GetMapping("/{healthElementId}")
	fun getHealthElement(
		@PathVariable healthElementId: String,
	): Mono<HealthElementDto> = mono {
		val element =
			healthElementService.getHealthElement(healthElementId)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting health element failed. Possible reasons: no such health element exists, or server error. Please try again or read the server log.",
				)

		healthElementV2Mapper.map(element)
	}

	@Operation(summary = "Get healthElements by batch", description = "Get a list of healthElement by ids/keys.")
	@PostMapping("/byIds")
	fun getHealthElements(
		@RequestBody healthElementIds: ListOfIdsDto,
	): Flux<HealthElementDto> {
		require(healthElementIds.ids.isNotEmpty()) { "You must specify at least one id." }
		return healthElementService.getHealthElements(healthElementIds.ids).map(healthElementV2Mapper::map).injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listHealthElementIdsByDataOwnerPatientOpeningDate instead")
	@Operation(
		summary = "List health elements found By Healthcare Party and secret foreign key element ids.",
		description = "Keys must be delimited by comma",
	)
	@GetMapping("/byHcPartySecretForeignKeys")
	fun listHealthElementsByHCPartyAndPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
	): Flux<HealthElementDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val elementList = healthElementService.listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)

		return elementList
			.map { element -> healthElementV2Mapper.map(element) }
			.injectReactorContext()
	}

	@Operation(summary = "Find Health Element ids by data owner id, patient secret keys and opening date")
	@PostMapping("/byDataOwnerPatientOpeningDate", produces = [APPLICATION_JSON_VALUE])
	fun listHealthElementIdsByDataOwnerPatientOpeningDate(
		@RequestParam dataOwnerId: String,
		@RequestParam(required = false) startDate: Long?,
		@RequestParam(required = false) endDate: Long?,
		@RequestParam(required = false) descending: Boolean?,
		@RequestBody secretPatientKeys: ListOfIdsDto,
	): Flux<String> {
		require(secretPatientKeys.ids.isNotEmpty()) {
			"You need to provide at least one secret patient key"
		}
		return healthElementService
			.listHealthElementIdsByDataOwnerPatientOpeningDate(
				dataOwnerId = dataOwnerId,
				secretForeignKeys = secretPatientKeys.ids.toSet(),
				startDate = startDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				endDate = endDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				descending = descending ?: false,
			).injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listHealthElementIdsByDataOwnerPatientOpeningDate instead")
	@Operation(summary = "List healthcare elements found By Healthcare Party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findHealthElementsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<HealthElementDto> {
		val elementList = healthElementService.listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)

		return elementList
			.map { element -> healthElementV2Mapper.map(element) }
			.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listHealthElementsDelegationsStubById instead")
	@Operation(
		summary = "List helement stubs found By Healthcare Party and secret foreign keys.",
		description = "Keys must be delimited by comma",
	)
	@GetMapping("/byHcPartySecretForeignKeys/delegations")
	fun listHealthElementsDelegationsStubsByHCPartyAndPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
	): Flux<IcureStubDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		return healthElementService
			.listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)
			.map { healthElement -> stubV2Mapper.mapToStub(healthElement) }
			.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listHealthElementsDelegationsStubById instead")
	@Operation(summary = "List helement stubs found By Healthcare Party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun findHealthElementsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> = healthElementService
		.listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)
		.map { healthElement -> stubV2Mapper.mapToStub(healthElement) }
		.injectReactorContext()

	@Operation(summary = "List health element stubs found by ids.")
	@PostMapping("/delegations")
	fun listHealthElementsDelegationsStubById(
		@RequestBody healthElementIds: ListOfIdsDto,
	): Flux<IcureStubDto> = healthElementService
		.getHealthElements(healthElementIds.ids)
		.map { healthElement -> stubV2Mapper.mapToStub(healthElement) }
		.injectReactorContext()

	@Operation(summary = "Deletes multiple HealthElements")
	@PostMapping("/delete/batch")
	fun deleteHealthElements(
		@RequestBody healthElementIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = healthElementService
		.deleteHealthElements(
			healthElementIds.ids.map { IdAndRev(it, null) },
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectReactorContext()

	@Operation(summary = "Deletes a multiple HealthElements if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteHealthElementsWithRev(
		@RequestBody healthElementIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = healthElementService
		.deleteHealthElements(
			healthElementIds.ids.map(idWithRevV2Mapper::map),
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectReactorContext()

	@Operation(summary = "Deletes an HealthElement")
	@DeleteMapping("/{healthElementId}")
	fun deleteHealthElement(
		@PathVariable healthElementId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = mono {
		healthElementService.deleteHealthElement(healthElementId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{healthElementId}")
	fun undeleteHealthElement(
		@PathVariable healthElementId: String,
		@RequestParam(required = true) rev: String,
	): Mono<HealthElementDto> = mono {
		healthElementV2Mapper.map(healthElementService.undeleteHealthElement(healthElementId, rev))
	}

	@DeleteMapping("/purge/{healthElementId}")
	fun purgeHealthElement(
		@PathVariable healthElementId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = mono {
		healthElementService.purgeHealthElement(healthElementId, rev).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Modify a health element", description = "Returns the modified health element.")
	@PutMapping
	fun modifyHealthElement(
		@RequestBody healthElementDto: HealthElementDto,
	): Mono<HealthElementDto> = mono {
		val modifiedHealthElement =
			healthElementService.modifyHealthElement(healthElementV2Mapper.map(healthElementDto))
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Health element modification failed.")
		healthElementV2Mapper.map(modifiedHealthElement)
	}

	@Operation(summary = "Modify a batch of health elements", description = "Returns the modified health elements.")
	@PutMapping("/batch")
	fun modifyHealthElements(
		@RequestBody healthElementDtos: List<HealthElementDto>,
	): Flux<HealthElementDto> = try {
		val hes = healthElementService.modifyEntities(
			healthElementDtos.map { f -> healthElementV2Mapper.map(f) }.asFlow()
		)
		hes.map { healthElementV2Mapper.map(it) }.injectReactorContext()
	} catch (e: Exception) {
		logger.warn(e.message, e)
		throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
	}

	@Operation(summary = "Create a batch of healthcare elements", description = "Returns the created healthcare elements.")
	@PostMapping("/batch")
	fun createHealthElements(
		@RequestBody healthElementDtos: List<HealthElementDto>,
	): Flux<HealthElementDto> = try {
		val hes = healthElementService.createEntities(
			healthElementDtos.map { f -> healthElementV2Mapper.map(f) }.asFlow()
		)
		hes.map { healthElementV2Mapper.map(it) }.injectReactorContext()
	} catch (e: Exception) {
		logger.warn(e.message, e)
		throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
	}

	@Operation(
		summary = "Filter health elements for the current user (HcParty)",
		description = "Returns a list of health elements along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.",
	)
	@PostMapping("/filter")
	fun filterHealthElementsBy(
		@Parameter(description = "A HealthElement document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<HealthElementDto>,
	): Mono<PaginatedList<HealthElementDto>> = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)

		val healthElements = healthElementService.filter(paginationOffset, filterChainV2Mapper.tryMap(filterChain).orThrow())

		healthElements.paginatedList(healthElementV2Mapper::map, realLimit, objectMapper = objectMapper)
	}

	@Operation(description = "Shares one or more health elements with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<HealthElementDto>> = flow {
		emitAll(
			healthElementService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it) },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(summary = "Get the ids of the Health Elements matching the provided filter.")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchHealthElementsBy(
		@RequestBody filter: AbstractFilterDto<HealthElementDto>,
	): Flux<String> = healthElementService
		.matchHealthElementsBy(
			filter = filterV2Mapper.tryMap(filter).orThrow(),
		).injectReactorContext()

	@Operation(description = "Shares one or more health elements with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<Nothing>> = flow {
		emitAll(
			healthElementService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it).minimal() },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
