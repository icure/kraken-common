/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.taktik.icure.asyncservice.HealthElementService
import org.taktik.icure.asyncservice.createEntities
import org.taktik.icure.asyncservice.modifyEntities
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.services.external.rest.v1.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v1.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v1.mapper.HealthElementMapper
import org.taktik.icure.services.external.rest.v1.mapper.StubMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterChainMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterMapper
import org.taktik.icure.services.external.rest.v1.utils.paginatedList
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import org.taktik.icure.utils.warn
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/helement")
@Tag(name = "helement")
class HealthElementController(
	private val healthElementService: HealthElementService,
	private val healthElementMapper: HealthElementMapper,
	private val delegationMapper: DelegationMapper,
	private val filterChainMapper: FilterChainMapper,
	private val filterMapper: FilterMapper,
	private val stubMapper: StubMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
	private val paginationConfig: SharedPaginationConfig
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(
		summary = "Create a healthcare element with the current user",
		description = "Returns an instance of created healthcare element."
	)
	@PostMapping
	fun createHealthElement(@RequestBody c: HealthElementDto) = mono {
		val element = healthElementService.createHealthElement(healthElementMapper.map(c))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Health element creation failed.")

		healthElementMapper.map(element)
	}

	@Operation(summary = "Get a healthcare element")
	@GetMapping("/{healthElementId}")
	fun getHealthElement(@PathVariable healthElementId: String) = mono {
		val element = healthElementService.getHealthElement(healthElementId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting healthcare element failed. Possible reasons: no such healthcare element exists, or server error. Please try again or read the server log.")

		healthElementMapper.map(element)
	}

	@Operation(summary = "Get healthElements by batch", description = "Get a list of healthElement by ids/keys.")
	@PostMapping("/byIds")
	fun getHealthElements(@RequestBody healthElementIds: ListOfIdsDto): Flux<HealthElementDto> = flow {
		val healthElements = healthElementService.getHealthElements(healthElementIds.ids)
		emitAll(healthElements.map { c -> healthElementMapper.map(c) })
	}.injectReactorContext()

	@Operation(summary = "List healthcare elements found By Healthcare Party and secret foreign key element ids.", description = "Keys hast to delimited by comma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findHealthElementsByHCPartyPatientForeignKeys(@RequestParam hcPartyId: String, @RequestParam secretFKeys: String): Flux<HealthElementDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val elementList = healthElementService.listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)

		return elementList
			.map { element -> healthElementMapper.map(element) }
			.injectReactorContext()
	}

	@Operation(summary = "List healthcare elements found By Healthcare Party and secret foreign key element ids.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findHealthElementsByHCPartyPatientForeignKeys(@RequestParam hcPartyId: String, @RequestBody secretPatientKeys: List<String>): Flux<HealthElementDto> {
		val elementList = healthElementService.listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)

		return elementList
			.map { element -> healthElementMapper.map(element) }
			.injectReactorContext()
	}

	@Operation(summary = "List helement stubs found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by comma")
	@GetMapping("/byHcPartySecretForeignKeys/delegations")
	fun findHealthElementsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String
	): Flux<IcureStubDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		return healthElementService.listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)
			.map { healthElement -> stubMapper.mapToStub(healthElement) }
			.injectReactorContext()
	}

	@Operation(summary = "List helement stubs found By Healthcare Party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys/delegations")
	fun findHealthElementsDelegationsStubsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestBody secretPatientKeys: List<String>,
	): Flux<IcureStubDto> {
		return healthElementService.listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)
			.map { healthElement -> stubMapper.mapToStub(healthElement) }
			.injectReactorContext()
	}

	@Operation(summary = "Update delegations in healthElements.")
	@PostMapping("/delegations")
	fun setHealthElementsDelegations(@RequestBody stubs: List<IcureStubDto>) = flow {
		val healthElements = healthElementService.getHealthElements(stubs.map { it.id }).map { he ->
			stubs.find { s -> s.id == he.id }?.let { stub ->
				he.copy(
					delegations = he.delegations.mapValues { (s, dels) -> stub.delegations[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.delegations.filterKeys { k -> !he.delegations.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					encryptionKeys = he.encryptionKeys.mapValues { (s, dels) -> stub.encryptionKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.encryptionKeys.filterKeys { k -> !he.encryptionKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					cryptedForeignKeys = he.cryptedForeignKeys.mapValues { (s, dels) -> stub.cryptedForeignKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
						stub.cryptedForeignKeys.filterKeys { k -> !he.cryptedForeignKeys.containsKey(k) }.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
				)
			} ?: he
		}
		emitAll(healthElementService.modifyEntities(healthElements).map { healthElementMapper.map(it) })
	}.injectReactorContext()

	@Operation(summary = "Delete healthcare elements.", description = "Response is a set containing the ID's of deleted healthcare elements.")
	@DeleteMapping("/{healthElementIds}")
	fun deleteHealthElements(@PathVariable healthElementIds: String): Flux<DocIdentifierDto> = flow {
		val ids = healthElementIds.split(',')
		if (ids.isEmpty()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		}

		emitAll(healthElementService.deleteHealthElements(ids.toSet()))
	}.map(docIdentifierMapper::map)
		.injectReactorContext()

	@Operation(summary = "Modify a healthcare element", description = "Returns the modified healthcare element.")
	@PutMapping
	fun modifyHealthElement(@RequestBody healthElementDto: HealthElementDto) = mono {
		val modifiedHealthElement = healthElementService.modifyHealthElement(healthElementMapper.map(healthElementDto))
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Health element modification failed.")
		healthElementMapper.map(modifiedHealthElement)
	}

	@Operation(summary = "Modify a batch of healthcare elements", description = "Returns the modified healthcare elements.")
	@PutMapping("/batch")
	fun modifyHealthElements(@RequestBody healthElementDtos: List<HealthElementDto>): Flux<HealthElementDto> = flow {
		try {
			val hes = healthElementService.modifyEntities(healthElementDtos.map { f -> healthElementMapper.map(f) })
			emitAll(hes.map { healthElementMapper.map(it) })
		} catch (e: Exception) {
			logger.warn(e) { e.message }
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
	}.injectReactorContext()

	@Operation(summary = "Create a batch of healthcare elements", description = "Returns the created healthcare elements.")
	@PostMapping("/batch")
	fun createHealthElements(@RequestBody healthElementDtos: List<HealthElementDto>): Flux<HealthElementDto> = flow {
		try {
			val hes = healthElementService.createEntities(healthElementDtos.map { f -> healthElementMapper.map(f) })
			emitAll(hes.map { healthElementMapper.map(it) })
		} catch (e: Exception) {
			logger.warn(e) { e.message }
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
	}.injectReactorContext()

	@Operation(summary = "Delegates a healthcare element to a healthcare party", description = "It delegates a healthcare element to a healthcare party (By current healthcare party). Returns the element with new delegations.")
	@PostMapping("/{healthElementId}/delegate")
	fun newHealthElementDelegations(@PathVariable healthElementId: String, @RequestBody ds: List<DelegationDto>) = mono {
		healthElementService.addDelegations(healthElementId, ds.map { d -> delegationMapper.map(d) })
		val healthElementWithDelegation = healthElementService.getHealthElement(healthElementId)

		val succeed = healthElementWithDelegation?.delegations != null && healthElementWithDelegation.delegations.isNotEmpty()
		if (succeed) {
			healthElementWithDelegation?.let { healthElementMapper.map(it) }
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delegation creation for healthcare element failed.")
		}
	}

	@Operation(
		summary = "Filter health elements for the current user (HcParty)",
		description = "Returns a list of health elements along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page."
	)
	@PostMapping("/filter")
	fun filterHealthElementsBy(
		@Parameter(description = "A HealthElement document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<HealthElementDto>
	) = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)

		val healthElements = healthElementService.filter(paginationOffset, filterChainMapper.tryMap(filterChain).orThrow())

		healthElements.paginatedList(healthElementMapper::map, realLimit)
	}

	@Operation(summary = "Get ids of health element matching the provided filter for the current user (HcParty) ")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchHealthElementsBy(
		@RequestBody filter: AbstractFilterDto<HealthElementDto>,
		@RequestParam(required = false) deduplicate: Boolean? = null
	) = healthElementService.matchHealthElementsBy(
		filter = filterMapper.tryMap(filter).orThrow(),
		deduplicate = deduplicate ?: false
	).injectReactorContext()

}
